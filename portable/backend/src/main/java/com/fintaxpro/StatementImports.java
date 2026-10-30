package com.fintaxpro;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
/** Atomic import of reviewed records parsed locally by the shared React CSV/XLSX reader. */
public final class StatementImports {
 static String normalize(String s){return Normalizer.normalize(s,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[|\\s]+"," ").trim();}
 static String contentKey(Finance.Tx t){return String.join("|",t.accountId(),t.date(),String.valueOf(t.amount()),t.direction(),normalize(t.description()));}
 static String referenceKey(Finance.Tx t){return t.reference()==null||t.reference().isBlank()?"":String.join("|",t.accountId(),t.date(),String.valueOf(t.amount()),t.direction(),normalize(t.reference()));}
 static String duplicate(List<Finance.Tx> existing,Finance.Tx row){String ref=referenceKey(row),content=contentKey(row);for(Finance.Tx t:existing){if(!t.accountId().equals(row.accountId()))continue;String tr=t.importReferenceKey()!=null&&!t.importReferenceKey().isBlank()?t.importReferenceKey():referenceKey(t);if(!ref.isEmpty()&&!tr.isEmpty()&&ref.equals(tr))return "reference";String tc=t.importKey()!=null&&!t.importKey().isBlank()?t.importKey():contentKey(t);if((ref.isEmpty()||tr.isEmpty())&&tc.equals(content))return "possible";}return null;}
 static String limited(Map<String,Object>d,String key,int max){String v=Finance.str(d,key);if(v.length()>max)throw new IllegalArgumentException("Field too long: "+key);return v;}
 @SuppressWarnings("unchecked") public static Finance.State apply(Finance.State s,Map<String,Object>d){
 String accountId=Finance.account(s,d);if(!Boolean.TRUE.equals(d.get("reviewAcknowledged")))throw new IllegalArgumentException("Review selected rows, classifications and opening balance before importing.");
 String batchId=limited(d,"requestId",80),fileName=limited(d,"fileName",120);
 if(s.importHistory().stream().anyMatch(b->b.id().equals(batchId)))throw new IllegalArgumentException("Import request already saved. Refresh your ledger.");
 if(!(d.get("rows") instanceof List<?> rows)||rows.isEmpty()||rows.size()>200)throw new IllegalArgumentException("Select between 1 and 200 rows per import.");
 List<Finance.Tx> additions=new ArrayList<>();int skipped=0;
 String asOf=s.accounts().stream().filter(a->a.id().equals(accountId)).findFirst().orElseThrow().asOf();
 for(Object item:rows){if(!(item instanceof Map<?,?>))throw new IllegalArgumentException("Invalid import row.");Map<String,Object> r=(Map<String,Object>)item;String date=Finance.date(r,"date");if(date.compareTo(asOf)<0||date.compareTo(Finance.today().toString())>0)throw new IllegalArgumentException("Imported dates must be between opening date and today.");String reference=r.get("reference")==null||"".equals(r.get("reference"))?"":limited(r,"reference",120);
 Finance.Tx t=new Finance.Tx(Finance.id(),accountId,Finance.str(r,"description"),Finance.amount(r.get("amount"),false),Finance.opt(r,"direction","INCOME","EXPENSE"),Finance.opt(r,"scope","PERSONAL","BUSINESS"),limited(r,"category",60),date,"Bank statement",null,null,reference,null,null,batchId);
 List<Finance.Tx> combined=new ArrayList<>(s.transactions());combined.addAll(additions);String dup=duplicate(combined,t);if("reference".equals(dup)||(dup!=null&&!Boolean.TRUE.equals(r.get("allowDuplicate")))){skipped++;continue;}
 String commitmentId=null,occurrence=null;if(r.get("commitmentId")!=null||r.get("occurrence")!=null){occurrence=Finance.date(r,"occurrence");String occ=occurrence;Finance.State interim=new Finance.State(s.profile(),s.accounts(),combined,s.commitments(),s.pots(),s.allocations(),s.runs(),s.taxEstimates(),s.dismissed(),s.importHistory());Finance.Event event=Finance.events(interim,Finance.today().plusDays(90)).stream().filter(e->e.schedule().id().equals(r.get("commitmentId"))&&e.occurrence().equals(occ)).findFirst().orElseThrow(()->new IllegalArgumentException("Commitment match is no longer available."));if(!event.schedule().accountId().equals(accountId)||event.schedule().amount()!=t.amount()||!event.schedule().direction().equals(t.direction()))throw new IllegalArgumentException("Commitment account, amount or direction differs.");commitmentId=event.schedule().id();}
 additions.add(new Finance.Tx(t.id(),accountId,t.description(),t.amount(),t.direction(),t.scope(),t.category(),date,t.method(),commitmentId,occurrence,reference,contentKey(t),referenceKey(t),batchId));}
 if(additions.isEmpty())throw new IllegalArgumentException("No new transactions. Matching bank references are never imported twice.");if(s.transactions().size()+additions.size()>5000)throw new IllegalArgumentException("Import exceeds the 5,000 transaction limit.");
 s.transactions().addAll(0,additions);Finance.checkReserves(s);s.importHistory().add(0,new Finance.ImportBatch(batchId,fileName,Instant.now().toString(),accountId,additions.size(),skipped));if(s.importHistory().size()>50)s.importHistory().remove(s.importHistory().size()-1);return s;
 }
}

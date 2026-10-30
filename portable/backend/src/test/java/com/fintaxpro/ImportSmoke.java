package com.fintaxpro;
import java.util.*;
/** Dependency-free domain smoke checks, runnable using the JDK compiler module. */
public final class ImportSmoke {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static void rejects(Runnable action){try{action.run();throw new AssertionError("Expected validation failure");}catch(IllegalArgumentException expected){}}
 static Finance.State fixture(){return Finance.apply(Finance.empty("Test"),"account",Map.of("name","Bank","opening",1000000,"asOf",Finance.today().minusDays(30).toString()));}
 static Map<String,Object> row(String reference){return new HashMap<>(Map.of("description","Client project","date",Finance.today().toString(),"amount",123456,"direction","INCOME","scope","BUSINESS","category","Client payment","reference",reference));}
 static Map<String,Object> batch(Finance.State s,List<Map<String,Object>> rows,String id){return Map.of("accountId",s.accounts().get(0).id(),"fileName","smoke.csv","requestId",id,"reviewAcknowledged",true,"rows",rows);}
 public static void main(String[] args){
 Finance.State s=fixture();Finance.State imported=Finance.apply(s,"importStatement",batch(s,List.of(row("REF"),row("REF")),"b1"));check(imported.transactions().size()==1,"reference de-duplication");check(Finance.balance(imported)==1123456,"exact paise");check(imported.importHistory().get(0).duplicatesSkipped()==1,"history counts");check(s.transactions().isEmpty(),"source state unchanged");rejects(()->Finance.apply(imported,"importStatement",batch(imported,List.of(row("REF")),"b1")));
 Map<String,Object> bad=row("BAD");bad.put("amount",-1);rejects(()->Finance.apply(s,"importStatement",batch(s,List.of(row("GOOD"),bad),"bad")));check(s.transactions().isEmpty(),"atomic rollback");
 Map<String,Object> repeat=row("");repeat.put("allowDuplicate",true);Finance.State repeats=Finance.apply(s,"importStatement",batch(s,List.of(row(""),repeat),"repeat"));check(repeats.transactions().size()==2,"explicit repeated payment");
 Finance.State legacy=new Finance.State(s.profile(),s.accounts(),s.transactions(),s.commitments(),s.pots(),s.allocations(),s.runs(),s.taxEstimates(),s.dismissed(),null);Finance.State restored=Finance.apply(legacy,"importStatement",batch(legacy,List.of(row("LEGACY")),"legacy"));check(restored.importHistory().size()==1,"legacy history migration");
 Finance.State scheduled=Finance.apply(s,"commitment",Map.of("accountId",s.accounts().get(0).id(),"description","Client project","amount",123456,"direction","INCOME","scope","BUSINESS","category","Client payment","date",Finance.today().plusDays(2).toString(),"frequency","NONE","priority","OBLIGATION"));Map<String,Object> matched=row("MATCH");matched.put("commitmentId",scheduled.commitments().get(0).id());matched.put("occurrence",scheduled.commitments().get(0).date());Finance.State settled=Finance.apply(scheduled,"importStatement",batch(scheduled,List.of(matched),"match"));check(Finance.events(settled,Finance.today().plusDays(7)).isEmpty(),"commitment settled once");
 Finance.State edited=Finance.apply(imported,"transaction",Map.of("id",imported.transactions().get(0).id(),"accountId",imported.accounts().get(0).id(),"description","Edited memo","amount",223456,"direction","INCOME","scope","BUSINESS","category","Client payment","date",Finance.today().toString()));rejects(()->Finance.apply(edited,"importStatement",batch(edited,List.of(row("REF")),"again")));
 System.out.println("PASS: Java import domain — exact paise, duplicate references, repeat payments, rollback, legacy workspaces, commitment settlement and preserved identities.");
 }
}

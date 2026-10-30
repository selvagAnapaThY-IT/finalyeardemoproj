export type ImportIdentity={accountId:string;date:string;amount:number;direction:string;description:string;reference?:string;importKey?:string;importReferenceKey?:string};
export const normalizeIdentity=(v:string)=>v.normalize('NFKC').toLowerCase().replace(/[|\s]+/g,' ').trim();
export function contentKey(t:ImportIdentity){return [t.accountId,t.date,t.amount,t.direction,normalizeIdentity(t.description)].join('|')}
export function referenceKey(t:ImportIdentity){return t.reference?.trim()?[t.accountId,t.date,t.amount,t.direction,normalizeIdentity(t.reference)].join('|'):''}
export function duplicateKind(existing:ImportIdentity[],row:ImportIdentity):'reference'|'possible'|null{const ref=referenceKey(row),key=contentKey(row);for(const t of existing){if(t.accountId!==row.accountId)continue;const tref=t.importReferenceKey||referenceKey(t);if(ref&&tref&&ref===tref)return 'reference';if((!ref||!tref)&&((t.importKey||contentKey(t))===key))return 'possible'}return null}

import {copyFileSync,mkdirSync} from 'node:fs';
mkdirSync('portable/frontend/lib',{recursive:true});
copyFileSync('app/workspace.tsx','portable/frontend/src/workspace.tsx');
copyFileSync('app/globals.css','portable/frontend/src/globals.css');
copyFileSync('lib/finance.ts','portable/frontend/lib/finance.ts');

copyFileSync('app/statement-import.tsx','portable/frontend/src/statement-import.tsx');
copyFileSync('lib/statement.ts','portable/frontend/lib/statement.ts');
copyFileSync('lib/import-keys.ts','portable/frontend/lib/import-keys.ts');
mkdirSync('portable/frontend/public/samples',{recursive:true});
copyFileSync('public/samples/bank-statement.csv','portable/frontend/public/samples/bank-statement.csv');

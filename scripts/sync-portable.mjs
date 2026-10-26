import {copyFileSync,mkdirSync} from 'node:fs';
mkdirSync('portable/frontend/lib',{recursive:true});
copyFileSync('app/workspace.tsx','portable/frontend/src/workspace.tsx');
copyFileSync('app/globals.css','portable/frontend/src/globals.css');
copyFileSync('lib/finance.ts','portable/frontend/lib/finance.ts');

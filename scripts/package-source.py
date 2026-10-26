from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
root=Path(__file__).resolve().parents[1]
include=['app','lib','db','drizzle','tests','portable','worker','scripts','public','vendor']
exclude={'node_modules','dist','target','__pycache__','.sites-runtime','.wrangler','.git','.vite-temp'}
files=[]
for dirname in include:
 for p in (root/dirname).rglob('*'):
  if not p.is_file() or any(x in exclude for x in p.relative_to(root).parts):continue
  if p.name=='FinTaxPro-source.zip' or p.name=='.env' or p.suffix in {'.pyc','.joblib'}:continue
  if 'datasets' in p.parts and p.suffix=='.csv':continue
  files.append(p)
for name in ['README.md','package.json','pnpm-lock.yaml','.gitignore','.npmrc','tsconfig.json','vite.config.ts','postcss.config.mjs','next.config.ts','cloudflare-env.d.ts','drizzle.config.ts']:
 if (root/name).is_file():files.append(root/name)
with ZipFile(root/'public'/'FinTaxPro-source.zip','w',ZIP_DEFLATED) as archive:
 for p in sorted(set(files)):archive.write(p,'FinTaxPro/'+str(p.relative_to(root)))
 archive.writestr('FinTaxPro/.openai/hosting.json','{\"d1\":\"DB\",\"r2\":null}\n')
print(f'Packaged {len(set(files))} source files without credentials, user data, build outputs or dependencies.')

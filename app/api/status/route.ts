import {env} from 'cloudflare:workers';
export async function GET(){const e=env as any;return Response.json({gemini:Boolean(e.GEMINI_API_KEY&&e.GEMINI_MODEL),ml:Boolean(e.ML_API_URL&&e.ML_API_KEY),taxYear:'AY 2026–27',forecast:'Deterministic scheduled cash flow'},{headers:{'Cache-Control':'no-store'}})}

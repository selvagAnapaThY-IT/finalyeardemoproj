import type { Metadata } from 'next';
import './globals.css';
export const metadata:Metadata={title:'FinTaxPro — Clarity for your next move',description:'Your finances, in focus. Manage transactions, build reserves and plan your cash flow.'};
export default function Layout({children}:{children:React.ReactNode}){return <html lang="en"><body>{children}</body></html>}

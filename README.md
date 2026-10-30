# FinTaxPro

A financial workspace for freelancers and independent professionals. The project includes a deployed-capable Sites edition and a portable React + Spring Boot + PostgreSQL edition.

## Working product features

- Dashboard with period filters, current total/unreserved funds, income, expenses, net flow and charts.
- Transaction create/edit/delete/search, personal/business classification, category suggestions and CSV export.
- CSV / Excel (.xlsx) bank-statement upload: local parsing, column mapping, review, duplicate detection, commitment matching, atomic import and import history.
- Multiple manually tracked accounts with explicit opening balance dates.
- Virtual reserve pots, target amounts, percentage guidelines, allocation/release history and over-allocation checks.
- One-time, weekly and monthly scheduled expenses and expected income; month-end recurrence handling.
- Settlement creates a single actual transaction; received income is excluded from future expected income. Removing a linked transaction makes that occurrence outstanding again.
- 7/14/30-day deterministic daily forecasts, minimum-buffer/negative-funds/overdue warnings and saved snapshots.
- Isolated what-if simulations for income delays, flexible-spend reductions and reserve releases.
- India FY 2025–26 / AY 2026–27 tax calculator, old/new slabs, resident rebate, new-regime marginal relief, 4% cess, ₹10 rounding and saved estimates.
- Financial reports, CSV export, browser print-to-PDF and workspace JSON backup.
- Server-side Gemini integration, optional authenticated classifier API integration and a clearly labeled offline product guide.
- Responsive React interface with explicit sample mode and a clean-workspace onboarding flow.

## Two runtime editions

| | Hosted Sites | Portable project |
|---|---|---|
| Frontend | React / Vinext, Tailwind pipeline, Recharts | React / Vite, Tailwind pipeline, Recharts |
| API | TypeScript / Cloudflare Worker | Java 17 / Spring Boot 3.5.5 |
| Persistence | D1, owner-keyed workspace records | PostgreSQL, users and versioned workspace records |
| Authentication | Platform-owned ChatGPT sign-in | BCrypt + signed JWT in HttpOnly/SameSite cookie |
| AI | Optional server credentials | Optional server credentials / FastAPI |

Sites cannot execute a Java process or host PostgreSQL. The hosted app uses a native server/storage adapter. The portable project is the requested Java/PostgreSQL deployment path and does not depend on ChatGPT sign-in.

### Persistence design

Financial data is a bounded aggregate with arrays of accounts, transactions, schedules and reserve movements. Hosted D1 uses `workspaces(user_id,payload,version,updated_at)`. The Java edition uses PostgreSQL `users` and `financial_workspaces`. This differs from the normalized multi-table schema proposed in the blueprint. Aggregate writes are atomic, scoped to the authenticated user, and version checked. Hosted writes use compare-and-swap; Java writes use a database lock and JPA version. Limits: 5,000 transactions, 500 schedules, 30 pots, 20 accounts, 3.5 MB payload. For larger workloads, migrate to normalized tables, server pagination and schema migrations before production rollout.

Money is represented as integer paise throughout. Java uses `BigDecimal` at numeric and percentage boundaries. No bank transfer or payment is performed. No bank credentials are collected.

## Hosted development

1. Install the repository's locked dependencies with the Sites helper (or the starter's install workflow).
2. Generate schema migrations with `npm run db:generate` only after schema changes.
3. Apply local migration `drizzle/0000_clever_maximus.sql` using the documented Sites D1 workflow.
4. Use `sites-preview start <checkout>` in the managed environment or the starter's `npm run dev` locally.
5. `node --experimental-strip-types --test tests/finance.test.ts` runs the finance tests on Node 24.
6. `node node_modules/typescript/bin/tsc --noEmit` checks the hosted sources.
7. Build with the Sites `build-site.mjs` helper and publish through the native Sites workflow.

Production Sites authentication supplies stable user headers; do not expose the Worker directly on a route that permits visitors to spoof those headers. Anonymous API requests are rejected. Optional secrets are `GEMINI_API_KEY`, `GEMINI_MODEL`, `ML_API_URL` and `ML_API_KEY`; configure them only on the server. Status labels indicate configured values, not a successful provider health check.

## Portable setup

See [portable/README.md](portable/README.md). The full source download is available from Settings in the app. `node scripts/sync-portable.mjs` copies the shared interface, CSS and client finance engine into the portable frontend after editing them. Do not hand-edit the copied files.

## AI/ML status and boundaries

The app works without AI credentials or trained models. It never presents keyword rules as a trained classifier or deterministic scheduled balances as Random Forest predictions.

- **Classification:** keyword suggestions run immediately. Configure a trained FastAPI service for TF-IDF + Logistic Regression. Users confirm or override suggestions; probabilities are not guarantees.
- **Gemini:** implemented server-side, but disabled until a key and model are configured. Questions only are transmitted, not the ledger. The advisor is educational and has no RAG collection or live verification.
- **Random Forest:** training and inference source is included for an unscheduled cash-flow component. It is not connected to the live forecast. Real historical data, out-of-time evaluation and a baseline comparison are required before adding it to user-facing forecasts. No trained artifact, accuracy claim or confidence interval is included.
- **Warnings:** deterministic rules, not ML.

## Tax scope and source

Rules verified against the Income Tax Department's [business/profession AY 2026–27 page](https://www.incometax.gov.in/iec/foportal/help/individual-business-profession) and [resident benefits reference](https://www.incometaxindia.gov.in/w/resident-benefits-allowable), accessed 26 October 2026. Rule ID: `india-ay2026-27-v1`.

Supported: resident individuals below 60, ordinary-rate income up to ₹50 lakh; new/old regime, manually entered eligible old-regime deductions, new-regime rebate and marginal relief, 4% cess. Taxable income rounds to ₹10, then final liability rounds to ₹10.

Not supported: GST calculations/returns, advance-tax instalments, capital gains or other special-rate income, salary standard deduction, new-regime deduction exceptions, surcharge, losses, presumptive taxation, TDS credits, senior citizen exemptions, other assessment years, filing, actual tax payment or bank connections. A personal/business label alone does not establish tax deductibility. Annual inputs are explicit rather than silently annualizing current-month transactions.

## Forecast assumptions

Current funds equal opening balances plus actual recorded cash movement. Expected income is not current funds. Reserves are already part of total funds. Unreserved equals total funds minus virtual reserves. All outstanding past-due commitments are included today. Monthly repeats clamp to the last date of shorter months without drifting. Forecasts show daily closing balances, include known scheduled flows only, and cannot detect intraday shortages or unrecorded spending. Income dates are assumptions, not guarantees. The simulator never writes back to the actual ledger. Forecast snapshots include schedule inputs, starting funds, reserve total, minimum threshold and calculated points.

## Verification

- 25 TypeScript finance/import tests pass for exact paise arithmetic, reserve invariants, settlement de-duplication, simulation isolation, ownership validation, monthly recurrence, tax boundaries and snapshot inputs.
- Hosted TypeScript checks and production build pass.
- Portable Vite frontend production build passes using the available shared packages.
- Python source syntax checks pass.
- Browser QA covers transaction save, classification suggestion, reserve allocation, forecast/simulator, tax estimator and report navigation. Browser checks also confirm CSV and XLSX upload, mapped previews, classification warnings, selected imports, exact decimal amounts and import receipts.
- The Java finance and statement-import domain classes compile through the installed JDK compiler module. The dependency-free `ImportSmoke` checks pass for exact paise, duplicate handling, rollback, legacy workspaces, preserved identities and commitment settlement. The full Spring Boot/PostgreSQL/Docker stack and JUnit suite remain unexecuted here because Maven and Docker are unavailable. Run `mvn verify` / Docker Compose before relying on that runtime. No external Gemini or trained ML call has been exercised without credentials/models.

## Before handling production finances

This is a project application, not a bank or tax filing service. Add production monitoring, rate limiting, tested backups, schema migrations (replace portable `ddl-auto=update`), recovery/version history, email verification and password reset, provider quota controls and a security review for the chosen deployment. These operations are not implemented by the current project. Review financial/tax rules with a qualified professional for your use case.

## Bank-statement upload and import

Open **Import statement** in the sidebar, or select **Import statement** on the dashboard / Smart Ledger.

1. Select a CSV or unprotected `.xlsx` file (maximum 5 MB, 2,000 transaction rows, 100 worksheet columns, 20 worksheets). PDF and legacy `.xls` are not supported. CSV must use UTF-8 encoding.
2. Choose the destination account and worksheet. Select the actual header row (within the first 20 rows). Column suggestions are editable. CSV separators: comma, semicolon or tab.
3. Map date/description and either separate debit-credit columns, amount + type, or a signed amount (positive credit, negative debit). Pick DMY, MDY or YMD for numeric dates. ISO dates and Excel date cells are recognized. Four-digit years are preferred; two-digit years map to 2000–2099. Amounts support INR prefixes, Indian/international comma grouping and decimal points, with at most two fractional digits.
4. Preview every extracted record. Keyword rules suggest purpose/category; this import flow does not call Gemini or the optional ML API. Transfers, refunds, loans, balance rows, unclear descriptions and duplicate candidates start unchecked. Invalid dates/amounts and conflicting debit/credit values cannot be selected. Edit or exclude uncertain classifications.
5. Optionally link a selected row to an outstanding commitment with the same account, amount and direction. This records settlement without double-counting expected income. The user must confirm the match; no automatic matching is claimed.
6. Confirm that the account opening balance predates the selected transactions and does not already include them, then import at most 200 selected rows per batch. All validation occurs server-side again. An invalid selected row aborts the entire batch. Reserve checks use the final batch balance, not transient row order.

The original file remains on the device. Only selected normalized transaction records, bank references, immutable duplicate identities, source batch IDs and a filename-based audit record are saved. No source file or model-provider upload is made. Imported rows are labeled in the ledger. Import history retains 50 batches and shows imported/skipped counts. Existing records and older workspaces remain compatible.

Duplicate detection is scoped to the destination account. A matching bank reference/date/amount/direction is skipped even if its description changed. Without references, matching date/amount/direction/normalized description is a **possible duplicate**; legitimate repeated payments can be explicitly selected after review. Duplicate identities survive ordinary edits to imported ledger rows. This is a heuristic, not a guarantee: providers can reuse references or vary statement descriptions/dates, so review overlapping statements and your account balance. Deleting an imported ledger transaction permits a later re-import; no deleted-record tombstone is retained.

Import is the authenticated workspace command `importStatement` in both the hosted TypeScript API and Java backend. The browser parses CSV/XLSX using the shared parser and ExcelJS; Java validates and persists normalized rows atomically in PostgreSQL. This is statement import, not live bank synchronization.

A fictional example is provided in `public/samples/bank-statement.csv` and the UI’s **Sample CSV** link. Source dates are October 2026; choose an account opening date on or before its first transaction when testing.


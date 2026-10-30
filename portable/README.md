# Run the React + Spring Boot + PostgreSQL edition

## Docker Compose

Requires Docker with Compose and outbound access to standard image/package registries.

1. Copy `.env.example` to `.env` in this directory.
2. Set a random `DATABASE_PASSWORD` and a different random `JWT_SECRET` of at least 32 characters. Example generator: `python -c "import secrets; print(secrets.token_urlsafe(48))"`.
3. Run `docker compose up --build` from this directory.
4. Open `http://localhost:3000`, register an account, choose **Start my workspace**, then add an opening account balance.

The frontend proxies `/api` to Spring Boot. PostgreSQL is internal and persists in a named Docker volume. The app tracks finances; it never moves real money. Sample data does not get copied to the real workspace.

The default local HTTP setup sets `COOKIE_SECURE=false`. For deployment behind HTTPS set it to true and set `FRONTEND_ORIGIN` to the actual origin. JWT cookies last one hour; log in again when expired. Do not expose the local sample environment publicly as a production deployment.

## Without Docker

- Install Java **JDK** 17 and Maven 3.9+, PostgreSQL and Node 24.
- Create a PostgreSQL database `fintaxpro` and an application user.
- Export `DATABASE_URL=jdbc:postgresql://localhost:5432/fintaxpro`, `DATABASE_USER`, `DATABASE_PASSWORD`, `JWT_SECRET`, `COOKIE_SECURE=false`, `FRONTEND_ORIGIN=http://localhost:5173` in the backend environment. Use the equivalent environment settings on Windows.
- In `backend`, run `mvn verify` then `mvn spring-boot:run`.
- In `frontend`, run `npm install` and `npm run dev`. Vite proxies API requests to `http://localhost:8080`.

## API contract

All monetary fields use integer **paise**. For ₹1,234.56 send `123456`. User IDs are taken only from the authenticated JWT, never from request bodies. `/api/workspace` returns `{state,version,user}`. POST to that route with `{type,data,version}`; stale writes return 409 and require refreshing. The browser consumes this aggregate API in both editions.

| Endpoint | Purpose |
|---|---|
| POST `/api/auth/register` | `{name,email,password}`; password 10–72 characters |
| POST `/api/auth/login` | `{email,password}`; sets HttpOnly JWT cookie |
| POST `/api/auth/logout` | Clears the cookie |
| GET `/api/users/me` | Current profile |
| GET `/api/workspace` | Read current user's financial aggregate |
| POST `/api/workspace` | Execute one validated, atomic command |
| GET `/api/status` | Configured integration capabilities |
| POST `/api/classify` | `{description,direction}`; ML when configured, rules otherwise |
| POST `/api/advisor` | `{question}`; Gemini when configured |

Commands: `initialize`, `profile`, `account`, `transaction`, `deleteTransaction`, `commitment`, `deleteCommitment`, `settle`, `pot`, `allocate`, `release`, `saveForecast`, `saveTax`, `dismiss`, `importStatement`. Payload shapes are defined by the shared UI and `Finance.java`. The API uses an aggregate command interface rather than the separate resource endpoints originally proposed in the blueprint.

## Gemini

Set `GEMINI_API_KEY` and `GEMINI_MODEL` on the backend only, then restart. Choose an available text model in your Google AI account; no model name is assumed. The interface changes to connected when values are set. Provider errors are handled and never substituted with fabricated model answers. No user ledger is sent to the model.

## Optional classifier service

1. Prepare independently labeled `ml/datasets/transactions.csv` as described in `ml/datasets/README.md`.
2. Create a Python virtual environment, install `ml/requirements.txt`.
3. From `ml`, run `python training/train_classifier.py datasets/transactions.csv`.
4. Inspect `saved_models/classifier-evaluation.json`. Only deploy if its real-data performance is suitable.
5. Set a random `ML_API_KEY` and `ML_API_URL=http://ml:8000` in `.env`.
6. Run `docker compose --profile ml up --build`.

For synthetic data, use `--synthetic` and keep the demonstration status explicit. No data or model is included by default. Joblib artifacts must come from trusted training; never accept arbitrary uploaded models.

## Optional Random Forest research component

`python training/train_forecaster.py datasets/unscheduled_daily.csv` trains with chronological holdout and recursive features. `/forecast` returns estimated **unscheduled** daily net flows in paise. It deliberately does not replace or integrate with the current user-facing deterministic forecast. Evaluate on sufficient history before introducing the hybrid forecast. Do not add scheduled transactions twice.

## Tests and known verification limit

`mvn verify` runs Java finance-domain tests; the backend Docker build executes them. The Java domain compiles and dependency-free import smoke checks pass. Full Spring compilation, JUnit integration tests, PostgreSQL behavior and the Compose stack still require execution with Maven or Docker. The React frontend build, hosted TypeScript engine tests and Python syntax were checked.

See the root README for supported financial features, tax scope, storage limits and omitted production operations.

## Import a bank statement

After adding an account, use **Import statement**. CSV and `.xlsx` are parsed locally in React. Review column mapping and selected rows before confirming. Spring Boot validates the normalized `importStatement` command, checks account ownership via the authenticated workspace, skips duplicates and writes the batch atomically. There is no separate file-upload endpoint or Apache POI requirement: the backend does not store raw bank files.

The command data contains `accountId`, `fileName`, a per-attempt `requestId`, `reviewAcknowledged: true`, and 1–200 `rows`. Each row has `date`, `description`, integer-paise `amount`, `direction`, `scope`, `category`, optional `reference`, optional `allowDuplicate`, and optional `commitmentId`/`occurrence`. Never send another user's account ID. Date and reserve constraints are enforced on the server.

`StatementImportTest` adds Java tests for atomicity, reference de-duplication, explicitly included repeat payments and legacy JSON compatibility. These Java tests still require Maven/JDK to execute. See the root README for limits and assumptions.

To run the dependency-free Java domain smoke checks without Maven, from the repository root:

```sh
java -m jdk.compiler/com.sun.tools.javac.Main -d /tmp/fintax-java portable/backend/src/main/java/com/fintaxpro/Finance.java portable/backend/src/main/java/com/fintaxpro/StatementImports.java portable/backend/src/test/java/com/fintaxpro/ImportSmoke.java
java -cp /tmp/fintax-java com.fintaxpro.ImportSmoke
```

Use a suitable output directory instead of `/tmp/fintax-java` on Windows. These checks do not replace `mvn verify` or a database-backed integration test.

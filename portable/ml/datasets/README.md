# Bring labeled data, not assumed accuracy
No real user financial data or pre-trained model is included.

Classifier CSV: `description,direction,scope,category`.
Directions: INCOME or EXPENSE. Scopes: PERSONAL or BUSINESS.
Allowed UI categories: Client payment, Salary, Other income, Rent, Utilities, Software, Food & groceries, Travel, Equipment, Other expense.
Use independently labeled descriptions (at least 20 per direction, much more recommended), then inspect the held-out evaluation. Do not put near-duplicate merchant templates in both training and test data. For stronger evaluation, split by merchant/source and time.

Forecaster CSV: `date,net_paise`, complete daily rows, at least 120 days. Values represent **unscheduled** net flows only, with known scheduled/recurring components removed. Do not fill missing observations with zero unless confirmed to be zero. The training script uses shifted rolling features and recursive chronological holdout, with a baseline comparison.

If using synthetic data, pass `--synthetic`. Its evaluation does not demonstrate performance on real finances. Never commit private financial datasets or model artifacts containing sensitive information.

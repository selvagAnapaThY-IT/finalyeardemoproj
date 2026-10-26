"""Train with independently labeled CSV: description,direction,scope,category.
Use --synthetic only for clearly identified demo data. No accuracy is assumed.
"""
import argparse, json
from pathlib import Path
from datetime import datetime, timezone
import joblib
import pandas as pd
from sklearn.pipeline import make_pipeline
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix

def main():
    p = argparse.ArgumentParser()
    p.add_argument("csv")
    p.add_argument("--synthetic", action="store_true")
    p.add_argument("--output", default="saved_models")
    args = p.parse_args()
    df = pd.read_csv(args.csv).dropna(subset=["description", "direction", "scope", "category"])
    df["description"] = df.description.str.strip().str.lower()
    # A description can never occur in both train and test, even if case varies.
    df = df.drop_duplicates(subset=["description", "direction"])
    if not set(df.direction).issubset({"INCOME", "EXPENSE"}) or not set(df.scope).issubset({"PERSONAL", "BUSINESS"}):
        raise ValueError("Invalid direction or scope labels")
    models, reports = {}, {}
    for direction in ["INCOME", "EXPENSE"]:
        subset = df[df.direction == direction]
        if len(subset) < 20 or subset.scope.nunique() < 2 or subset.scope.value_counts().min() < 4:
            raise ValueError(f"Need at least 20 independent {direction} descriptions, both scopes, and >=4 in each scope")
        train, test = train_test_split(subset, test_size=0.25, random_state=42, stratify=subset.scope)
        if train.category.nunique() < 2:
            raise ValueError(f"Need at least two training categories for {direction}")
        models[direction], reports[direction] = {}, {}
        for target in ["scope", "category"]:
            model = make_pipeline(TfidfVectorizer(ngram_range=(1, 2), sublinear_tf=True), LogisticRegression(max_iter=1500, class_weight="balanced", random_state=42))
            model.fit(train.description, train[target])
            predicted = model.predict(test.description)
            reports[direction][target] = {"report": classification_report(test[target], predicted, output_dict=True, zero_division=0),
                "labels": sorted(set(test[target]) | set(predicted)),
                "confusionMatrix": confusion_matrix(test[target], predicted, labels=sorted(set(test[target]) | set(predicted))).tolist(),
                "trainCount": len(train), "testCount": len(test)}
            models[direction][target] = model
    version = datetime.now(timezone.utc).isoformat()
    out = Path(args.output); out.mkdir(parents=True, exist_ok=True)
    joblib.dump({"models": models, "version": version, "synthetic": args.synthetic}, out / "classifier.joblib")
    (out / "classifier-evaluation.json").write_text(json.dumps({"version": version, "synthetic": args.synthetic, "results": reports}, indent=2))
    print("Saved model and held-out evaluation. Review the report before deploying.")

if __name__ == "__main__":
    main()

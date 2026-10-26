"""Time-ordered validation of unscheduled daily cash-flow only.
CSV columns: date,net_paise. Remove known recurring/scheduled components first.
This is a proposed ML component, not the deployed forecast engine.
"""
import argparse, json
from pathlib import Path
from datetime import datetime, timezone
import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error

def main():
    p = argparse.ArgumentParser(); p.add_argument("csv"); p.add_argument("--synthetic", action="store_true"); p.add_argument("--output", default="saved_models")
    args = p.parse_args()
    df = pd.read_csv(args.csv, parse_dates=["date"]).sort_values("date")
    if df.date.duplicated().any():
        raise ValueError("Provide one daily total per date")
    df = df.set_index("date").asfreq("D")
    if df.net_paise.isna().any():
        raise ValueError("Missing days: provide explicit zero values for confirmed days with no transactions")
    if len(df) < 120:
        raise ValueError("Need at least 120 complete days, preferably much more")
    net = df.net_paise
    features = pd.DataFrame({"weekday":df.index.dayofweek,"month":df.index.month,"lag1":net.shift(1),"mean7":net.shift(1).rolling(7).mean(),"mean30":net.shift(1).rolling(30).mean()},index=df.index)
    usable = features.dropna(); y=net.loc[usable.index]
    split = int(len(usable)*0.8)
    model=RandomForestRegressor(n_estimators=200,max_depth=8,min_samples_leaf=3,random_state=42,n_jobs=-1)
    model.fit(usable.iloc[:split].to_numpy(),y.iloc[:split])
    # Recursive held-out test: never use actual future net in lag features.
    cutoff=df.index.get_loc(usable.index[split]); history=net.iloc[:cutoff].tolist(); predicted=[]
    for day in usable.index[split:]:
        x=[[day.dayofweek,day.month,history[-1],sum(history[-7:])/7,sum(history[-30:])/30]]
        prediction=float(model.predict(x)[0]); predicted.append(prediction); history.append(prediction)
    actual=y.iloc[split:].to_numpy()
    baseline=np.full(len(actual),net.iloc[:cutoff].tail(30).mean())
    metrics={"synthetic":args.synthetic,"validation":"recursive chronological holdout","holdoutDays":len(actual),"MAE_paise":float(mean_absolute_error(actual,predicted)),"RMSE_paise":float(np.sqrt(mean_squared_error(actual,predicted))),"baseline_MAE_paise":float(mean_absolute_error(actual,baseline)),"trainedThrough":str(usable.index[split-1].date())}
    # Keep holdout separate; this artifact is the training-only model used in evaluation.
    out=Path(args.output);out.mkdir(parents=True,exist_ok=True)
    joblib.dump({"model":model,"version":datetime.now(timezone.utc).isoformat(),"synthetic":args.synthetic},out/"forecaster.joblib")
    (out/"forecaster-evaluation.json").write_text(json.dumps(metrics,indent=2))
    print(json.dumps(metrics,indent=2))

if __name__ == "__main__":
    main()

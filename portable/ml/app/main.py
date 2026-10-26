"""Optional ML inference service. It never downloads or trusts user-provided model files."""
from pathlib import Path
from typing import Literal
import os, secrets
import joblib
from fastapi import FastAPI, Depends, Header, HTTPException
from pydantic import BaseModel, Field

app = FastAPI(title="FinTaxPro ML", version="1.0.0")
ROOT = Path(__file__).resolve().parents[1]
MODEL_PATH = ROOT / "saved_models" / "classifier.joblib"
FORECAST_PATH = ROOT / "saved_models" / "forecaster.joblib"
classifier = joblib.load(MODEL_PATH) if MODEL_PATH.exists() else None
forecaster = joblib.load(FORECAST_PATH) if FORECAST_PATH.exists() else None

def auth(x_api_key: str = Header(default="")):
    key = os.environ.get("ML_API_KEY", "")
    if not key or not secrets.compare_digest(x_api_key, key):
        raise HTTPException(401, "Invalid service credentials")

class Transaction(BaseModel):
    description: str = Field(min_length=1, max_length=255)
    direction: Literal["INCOME", "EXPENSE"]

@app.get("/health")
def health():
    return {"classifierReady": classifier is not None, "forecasterReady": forecaster is not None}

@app.post("/classify", dependencies=[Depends(auth)])
def classify(t: Transaction):
    if classifier is None or t.direction not in classifier["models"]:
        raise HTTPException(503, "Train and mount the classifier first")
    models = classifier["models"][t.direction]
    scope = models["scope"].predict([t.description])[0]
    category = models["category"].predict([t.description])[0]
    confidence = float(models["scope"].predict_proba([t.description]).max())
    return {"scope": scope, "category": category, "confidence": confidence,
            "source": "ML", "model": "TFIDF_LOGISTIC_REGRESSION", "version": classifier["version"],
            "note": "Model probability is not a guarantee. Confirm before saving."}

class ForecastInput(BaseModel):
    # Oldest first; all monetary values are integer paise. Exclude scheduled flows.
    daily_net: list[int] = Field(min_length=30, max_length=730)
    start_date: str = Field(pattern=r"^\d{4}-\d{2}-\d{2}$")
    days: Literal[7, 14, 30] = 7

@app.post("/forecast", dependencies=[Depends(auth)])
def predict_forecast(data: ForecastInput):
    if forecaster is None:
        raise HTTPException(503, "Train and mount the forecaster first")
    import pandas as pd
    try:
        start = pd.Timestamp(data.start_date)
    except ValueError:
        raise HTTPException(400, "Invalid date")
    history = list(data.daily_net)
    out = []
    for i in range(data.days):
        day = start + pd.Timedelta(days=i)
        features = [[day.dayofweek, day.month, history[-1], sum(history[-7:]) / 7, sum(history[-30:]) / 30]]
        value = int(round(float(forecaster["model"].predict(features)[0])))
        out.append({"date": day.strftime("%Y-%m-%d"), "estimatedUnscheduledNetPaise": value})
        history.append(value)
    return {"predictions": out, "source": "ML", "model": "RANDOM_FOREST", "version": forecaster["version"],
            "note": "Unscheduled component only. Add known schedules separately. Not connected to the hosted deterministic forecast."}

from fastapi import FastAPI

app = FastAPI(title="ParkFlow Mall Vision Service", version="0.0.1")


@app.get("/health")
def health() -> dict[str, str]:
    """Foundation-only health endpoint; no OCR behavior is implemented."""
    return {"status": "UP", "service": "vision-service"}

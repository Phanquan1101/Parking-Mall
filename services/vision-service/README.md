# Vision Service

Responsibility: future OCR-assist plate candidate and confidence contract.

Current status: FastAPI skeleton only. `GET /health` is available; no OCR models, images, YOLO, EasyOCR, or PaddleOCR integrations are included.

Business logic starts: Slice 10 - OCR Assist.

Run locally:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8090
```

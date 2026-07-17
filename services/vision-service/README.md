# Vision Service

Responsibility: backend-only OCR-assist plate candidate and confidence contract.

Current status: Slice 10 supports deterministic `DEMO_OCR`, real `GEMINI`, and local CPU-only `PADDLE_OCR` providers. Images exist only for the duration of the request and are never persisted.

Business logic starts: Slice 10 - OCR Assist.

Run locally:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8090
```

Provider configuration:

```powershell
$env:VISION_OCR_PROVIDER="DEMO_OCR" # default; derives a deterministic candidate from a plate-like filename
$env:VISION_OCR_PROVIDER="GEMINI"
$env:GEMINI_API_KEY="your-server-side-key"
$env:GEMINI_MODEL="gemini-2.5-flash"
$env:VISION_OCR_PROVIDER="PADDLE_OCR" # local, no API key or external quota
$env:OCR_LOW_CONFIDENCE_THRESHOLD="0.75"
```

The Gemini API key is read only by this service. `PADDLE_OCR` runs locally on CPU, keeps its model in memory after the first request, and makes no external OCR calls. The initial model download is retained in the `paddleocr-model-cache` Docker volume, so normal service recreation does not need to download it again. Its Docker image pins the NumPy 1.x ABI required by its CPU OpenCV wheel. Landscape camera frames use one OCR pass for responsiveness; portrait uploads without a first-pass plate candidate use a rotation fallback. The frontend calls the normal OCR endpoint through the gateway and never receives the key. OCR suggestions always require staff confirmation or correction before a normal Parking check-in.

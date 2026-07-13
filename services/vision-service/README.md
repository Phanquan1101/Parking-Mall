# Vision Service

Responsibility: backend-only OCR-assist plate candidate and confidence contract.

Current status: Slice 10B supports both deterministic `DEMO_OCR` and real `GEMINI` OCR providers. Images exist only for the duration of the request and are never persisted.

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
$env:OCR_LOW_CONFIDENCE_THRESHOLD="0.75"
```

The Gemini API key is read only by this service. The frontend calls the normal OCR endpoint through the gateway and never receives the key. Gemini suggestions always require staff confirmation or correction before a normal Parking check-in.

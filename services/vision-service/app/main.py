import base64
import hashlib
import hmac
import json
import os
import re
import uuid
from datetime import datetime, timezone

from fastapi import FastAPI, File, Form, Header, HTTPException, UploadFile

app = FastAPI(title="ParkFlow Mall Vision Service", version="0.0.1")
JWT_SECRET = os.getenv("JWT_SECRET", "parkflow-local-development-secret-change-me-2026")
LOW_CONFIDENCE_THRESHOLD = float(os.getenv("OCR_LOW_CONFIDENCE_THRESHOLD", "0.75"))
SUPPORTED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp"}


def _decode_segment(segment: str) -> bytes:
    return base64.urlsafe_b64decode(segment + "=" * (-len(segment) % 4))


def _require_staff_token(authorization: str | None) -> None:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing access token")
    try:
        token = authorization[7:]
        header, payload, signature = token.split(".")
        algorithm = json.loads(_decode_segment(header)).get("alg")
        digest = {"HS256": hashlib.sha256, "HS384": hashlib.sha384, "HS512": hashlib.sha512}.get(algorithm)
        if digest is None:
            raise ValueError("unsupported algorithm")
        expected = hmac.new(JWT_SECRET.encode(), f"{header}.{payload}".encode(), digest).digest()
        if not hmac.compare_digest(expected, _decode_segment(signature)):
            raise ValueError("signature")
        claims = json.loads(_decode_segment(payload))
        if claims.get("exp") is not None and float(claims["exp"]) < datetime.now(timezone.utc).timestamp():
            raise ValueError("expired")
        roles = claims.get("roles") or []
        if not any(role in {"ADMIN", "PARKING_STAFF"} for role in roles):
            raise HTTPException(status_code=403, detail="Parking staff or admin role is required")
    except HTTPException:
        raise
    except (ValueError, KeyError, json.JSONDecodeError):
        raise HTTPException(status_code=401, detail="Invalid access token")


def _demo_candidate(filename: str | None) -> tuple[str | None, float]:
    # Filename parsing is deterministic for the demo; image bytes are never persisted.
    stem = (filename or "").upper()
    match = re.search(r"([0-9]{2}[A-Z][0-9][- ]?[0-9]{4,5})", stem)
    if not match:
        return None, 0.2
    candidate = match.group(1).replace(" ", "")
    return candidate, 0.82


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP", "service": "vision-service"}


@app.post("/api/vision/ocr/plate")
async def ocr_plate(
    image: UploadFile | None = File(default=None),
    cameraId: str | None = Form(default=None),
    gateId: str | None = Form(default=None),
    authorization: str | None = Header(default=None),
) -> dict:
    _require_staff_token(authorization)
    if image is None:
        raise HTTPException(status_code=400, detail="image is required")
    if image.content_type not in SUPPORTED_CONTENT_TYPES:
        raise HTTPException(status_code=400, detail="Unsupported image content type")
    candidate, confidence = _demo_candidate(image.filename)
    warnings = ["Staff confirmation is required before check-in."]
    if candidate is None or confidence < LOW_CONFIDENCE_THRESHOLD:
        warnings.append("OCR confidence is low; enter or correct the plate manually.")
    return {
        "ocrRequestId": str(uuid.uuid4()),
        "candidatePlate": candidate,
        "normalizedCandidatePlate": re.sub(r"[^A-Z0-9]", "", candidate) if candidate else None,
        "confidence": confidence,
        "provider": "DEMO_OCR",
        "warnings": warnings,
        "createdAt": datetime.now(timezone.utc).isoformat(),
    }

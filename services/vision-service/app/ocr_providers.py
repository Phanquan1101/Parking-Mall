"""Backend-only OCR provider implementations for the Vision Service."""

from __future__ import annotations

import json
import math
import re
from dataclasses import dataclass
from typing import Callable


STAFF_CONFIRMATION_WARNING = "Staff confirmation is required before check-in."


class OcrProviderConfigurationError(Exception):
    """Raised when a selected OCR provider is not safely configured."""


class OcrProviderUnavailableError(Exception):
    """Raised when a configured OCR provider cannot complete a request."""


@dataclass(frozen=True)
class OcrResult:
    candidate_plate: str | None
    normalized_candidate_plate: str | None
    confidence: float
    provider: str
    warnings: list[str]


def normalize_plate(value: str | None) -> str | None:
    if not isinstance(value, str):
        return None
    normalized = re.sub(r"[^A-Z0-9]", "", value.upper())
    return normalized or None


def clamp_confidence(value: object) -> float:
    try:
        confidence = float(value)
    except (TypeError, ValueError):
        return 0.0
    if not math.isfinite(confidence):
        return 0.0
    return max(0.0, min(1.0, confidence))


class DemoPlateOcrProvider:
    name = "DEMO_OCR"

    def recognize(self, image_bytes: bytes, content_type: str, filename: str | None) -> OcrResult:
        # Filename parsing is deterministic for local development; image bytes are not persisted.
        del image_bytes, content_type
        match = re.search(r"([0-9]{2}[A-Z][0-9][- ]?[0-9]{4,5})", (filename or "").upper())
        candidate = match.group(1).replace(" ", "") if match else None
        return OcrResult(
            candidate_plate=candidate,
            normalized_candidate_plate=normalize_plate(candidate),
            confidence=0.82 if candidate else 0.2,
            provider=self.name,
            warnings=[],
        )


class GeminiPlateOcrProvider:
    name = "GEMINI"
    prompt = """Analyze this vehicle image for a Vietnamese license plate. Return JSON only, with exactly these fields:
{
  \"plateNumber\": \"string or null\",
  \"normalizedPlate\": \"uppercase alphanumeric string or null\",
  \"confidence\": 0.0,
  \"vehicleType\": \"MOTORBIKE, CAR, or null\",
  \"warnings\": [\"string\"]
}
Extract only the license plate text. Support motorbike and car plates. Preserve useful punctuation in plateNumber, but normalizedPlate must remove spaces, hyphens, dots, newlines, and other non-alphanumeric characters. If the plate is not reliably readable, return null values and low confidence. Do not invent a plate."""

    def __init__(
        self,
        api_key: str,
        model: str,
        generate_content: Callable[[str, bytes, str, str], str] | None = None,
    ) -> None:
        if not api_key.strip():
            raise OcrProviderConfigurationError("GEMINI_API_KEY is required for the GEMINI provider")
        self.api_key = api_key
        self.model = model
        self._generate_content = generate_content or self._generate_with_sdk

    def recognize(self, image_bytes: bytes, content_type: str, filename: str | None) -> OcrResult:
        del filename
        try:
            raw_response = self._generate_content(self.model, image_bytes, content_type, self.prompt)
        except (OcrProviderUnavailableError, OcrProviderConfigurationError):
            raise
        except Exception as exc:  # Provider errors are deliberately not exposed to callers.
            raise OcrProviderUnavailableError("Gemini OCR request failed") from exc

        return self._result_from_response(raw_response)

    def _generate_with_sdk(self, model: str, image_bytes: bytes, content_type: str, prompt: str) -> str:
        try:
            from google import genai
            from google.genai import types

            client = genai.Client(api_key=self.api_key)
            response = client.models.generate_content(
                model=model,
                contents=[prompt, types.Part.from_bytes(data=image_bytes, mime_type=content_type)],
                config=types.GenerateContentConfig(response_mime_type="application/json", temperature=0),
            )
            return response.text or ""
        except ImportError as exc:
            raise OcrProviderConfigurationError("Gemini SDK is unavailable") from exc

    def _result_from_response(self, raw_response: str) -> OcrResult:
        try:
            payload = json.loads(_strip_markdown_fence(raw_response))
            if not isinstance(payload, dict):
                raise ValueError("response is not a JSON object")
            plate_number = payload.get("plateNumber")
            candidate = plate_number.strip().upper() if isinstance(plate_number, str) and plate_number.strip() else None
            warnings = _safe_warnings(payload.get("warnings"))
            return OcrResult(
                candidate_plate=candidate,
                normalized_candidate_plate=normalize_plate(candidate),
                confidence=clamp_confidence(payload.get("confidence")),
                provider=self.name,
                warnings=warnings,
            )
        except (TypeError, ValueError, json.JSONDecodeError):
            return OcrResult(
                candidate_plate=None,
                normalized_candidate_plate=None,
                confidence=0.0,
                provider=self.name,
                warnings=["Gemini returned an unreadable OCR response; enter the plate manually."],
            )


def create_ocr_provider(provider_name: str, api_key: str, model: str):
    selected_provider = provider_name.strip().upper()
    if selected_provider == DemoPlateOcrProvider.name:
        return DemoPlateOcrProvider()
    if selected_provider == GeminiPlateOcrProvider.name:
        return GeminiPlateOcrProvider(api_key=api_key, model=model)
    raise OcrProviderConfigurationError("VISION_OCR_PROVIDER must be DEMO_OCR or GEMINI")


def _strip_markdown_fence(value: object) -> str:
    if not isinstance(value, str):
        raise ValueError("response is not text")
    stripped = value.strip()
    if stripped.startswith("```") and stripped.endswith("```"):
        return "\n".join(stripped.splitlines()[1:-1]).strip()
    return stripped


def _safe_warnings(value: object) -> list[str]:
    if not isinstance(value, list):
        return []
    return [warning.strip() for warning in value if isinstance(warning, str) and warning.strip()]

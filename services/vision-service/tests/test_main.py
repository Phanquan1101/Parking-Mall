import base64
import hashlib
import hmac
import json
import os
import time
import unittest
from fastapi.testclient import TestClient
from app.main import app
from app.ocr_providers import GeminiPlateOcrProvider, PaddlePlateOcrProvider, create_ocr_provider
from unittest.mock import patch

client = TestClient(app)
secret = os.getenv("JWT_SECRET", "parkflow-local-development-secret-change-me-2026")


def token(role):
    enc = lambda value: base64.urlsafe_b64encode(json.dumps(value, separators=(",", ":")).encode()).decode().rstrip("=")
    header, payload = enc({"alg": "HS384", "typ": "JWT"}), enc({"sub": "test", "roles": [role], "exp": time.time() + 3600})
    signature = base64.urlsafe_b64encode(hmac.new(secret.encode(), f"{header}.{payload}".encode(), hashlib.sha384).digest()).decode().rstrip("=")
    return f"{header}.{payload}.{signature}"


def image(name="59A1-12345.jpg", content_type="image/jpeg"):
    return {"image": (name, b"demo", content_type)}


@patch.dict(os.environ, {"VISION_OCR_PROVIDER": "DEMO_OCR"})
class VisionOcrTests(unittest.TestCase):
    def test_auth_and_roles(self):
        self.assertEqual(401, client.post("/api/vision/ocr/plate", files=image()).status_code)
        self.assertEqual(403, client.post("/api/vision/ocr/plate", headers={"Authorization": "Bearer " + token("MERCHANT_STAFF")}, files=image()).status_code)
        self.assertEqual(200, client.post("/api/vision/ocr/plate", headers={"Authorization": "Bearer " + token("ADMIN")}, files=image()).status_code)
        self.assertEqual(200, client.post("/api/vision/ocr/plate", headers={"Authorization": "Bearer " + token("PARKING_STAFF")}, files=image()).status_code)

    def test_image_response_and_low_confidence_warning(self):
        headers = {"Authorization": "Bearer " + token("PARKING_STAFF")}
        self.assertEqual(400, client.post("/api/vision/ocr/plate", headers=headers).status_code)
        self.assertEqual(400, client.post("/api/vision/ocr/plate", headers=headers, files=image("plate.gif", "image/gif")).status_code)
        response = client.post("/api/vision/ocr/plate", headers=headers, files=image()).json()
        self.assertTrue(response["ocrRequestId"])
        self.assertEqual("DEMO_OCR", response["provider"])
        self.assertEqual("59A1-12345", response["candidatePlate"])
        self.assertEqual("59A112345", response["normalizedCandidatePlate"])
        self.assertTrue(0 <= response["confidence"] <= 1)
        self.assertIn("Staff confirmation", response["warnings"][0])
        low = client.post("/api/vision/ocr/plate", headers=headers, files=image("unknown.jpg")).json()
        self.assertIsNone(low["candidatePlate"])
        self.assertGreaterEqual(len(low["warnings"]), 2)

    def test_gemini_provider_parses_mocked_json_and_clamps_confidence(self):
        provider = GeminiPlateOcrProvider(
            "test-key",
            "gemini-test",
            generate_content=lambda *_: '{"plateNumber":"59a1-12345", "normalizedPlate":"ignored", "confidence":1.4, "vehicleType":"MOTORBIKE", "warnings":["Glare"]}',
        )
        result = provider.recognize(b"image", "image/jpeg", "plate.jpg")
        self.assertEqual("GEMINI", result.provider)
        self.assertEqual("59A1-12345", result.candidate_plate)
        self.assertEqual("59A112345", result.normalized_candidate_plate)
        self.assertEqual(1.0, result.confidence)
        self.assertEqual(["Glare"], result.warnings)

    def test_gemini_provider_handles_no_plate_and_invalid_json_safely(self):
        no_plate = GeminiPlateOcrProvider(
            "test-key", "gemini-test", generate_content=lambda *_: '{"plateNumber":null,"confidence":-1,"warnings":["No readable plate"]}'
        ).recognize(b"image", "image/jpeg", "unknown.jpg")
        self.assertIsNone(no_plate.candidate_plate)
        self.assertEqual(0.0, no_plate.confidence)

        invalid_json = GeminiPlateOcrProvider("test-key", "gemini-test", generate_content=lambda *_: "not-json").recognize(b"image", "image/jpeg", "unknown.jpg")
        self.assertIsNone(invalid_json.candidate_plate)
        self.assertEqual(0.0, invalid_json.confidence)
        self.assertIn("unreadable", invalid_json.warnings[0])

    def test_paddle_provider_combines_two_row_motorbike_plate(self):
        provider = PaddlePlateOcrProvider(
            recognize_text=lambda *_: [("89-F1", 0.96), ("555.55", 0.91)]
        )
        result = provider.recognize(b"frame", "image/jpeg", "frame.jpg")
        self.assertEqual("PADDLE_OCR", result.provider)
        self.assertEqual("89-F1 555.55", result.candidate_plate)
        self.assertEqual("89F155555", result.normalized_candidate_plate)
        self.assertEqual(0.91, result.confidence)
        self.assertEqual([], result.warnings)

    def test_paddle_provider_returns_manual_fallback_for_non_plate_text(self):
        provider = PaddlePlateOcrProvider(recognize_text=lambda *_: [("WELCOME", 0.99)])
        result = provider.recognize(b"frame", "image/jpeg", "frame.jpg")
        self.assertIsNone(result.candidate_plate)
        self.assertEqual(0.0, result.confidence)
        self.assertIn("enter it manually", result.warnings[0])

    def test_paddle_provider_accepts_plate_parts_from_rotated_portrait_capture(self):
        provider = PaddlePlateOcrProvider(
            recognize_text=lambda *_: [("89", 0.99), ("E1", 0.99), ("555.", 0.99), ("55", 0.99)]
        )
        result = provider.recognize(b"frame", "image/jpeg", "rotated.jpg")
        self.assertEqual("89-E1 555.55", result.candidate_plate)
        self.assertEqual("89E155555", result.normalized_candidate_plate)

    def test_paddle_provider_is_selectable_without_an_api_key(self):
        self.assertIsInstance(create_ocr_provider("PADDLE_OCR", "", "unused"), PaddlePlateOcrProvider)

    def test_gemini_missing_key_returns_safe_configuration_error(self):
        headers = {"Authorization": "Bearer " + token("PARKING_STAFF")}
        with patch.dict(os.environ, {"VISION_OCR_PROVIDER": "GEMINI", "GEMINI_API_KEY": ""}):
            response = client.post("/api/vision/ocr/plate", headers=headers, files=image())
        self.assertEqual(503, response.status_code)
        self.assertEqual("OCR provider configuration is unavailable", response.json()["detail"])

    def test_endpoint_uses_mocked_gemini_provider_without_network_and_keeps_warning(self):
        headers = {"Authorization": "Bearer " + token("PARKING_STAFF")}
        provider = GeminiPlateOcrProvider(
            "test-key", "gemini-test", generate_content=lambda *_: '{"plateNumber":"51F-123.45","confidence":0.86,"warnings":[]}'
        )
        with patch("app.main.create_ocr_provider", return_value=provider) as factory:
            response = client.post("/api/vision/ocr/plate", headers=headers, files=image("capture.jpg"))
        self.assertEqual(200, response.status_code)
        self.assertEqual("GEMINI", response.json()["provider"])
        self.assertEqual("51F12345", response.json()["normalizedCandidatePlate"])
        self.assertIn("Staff confirmation is required before check-in.", response.json()["warnings"])
        factory.assert_called_once()

    def test_endpoint_uses_mocked_paddle_provider_and_keeps_staff_confirmation(self):
        headers = {"Authorization": "Bearer " + token("PARKING_STAFF")}
        provider = PaddlePlateOcrProvider(recognize_text=lambda *_: [("89-F1", 0.95), ("555.55", 0.93)])
        with patch("app.main.create_ocr_provider", return_value=provider):
            response = client.post("/api/vision/ocr/plate", headers=headers, files=image("capture.jpg"))
        self.assertEqual(200, response.status_code)
        self.assertEqual("PADDLE_OCR", response.json()["provider"])
        self.assertEqual("89F155555", response.json()["normalizedCandidatePlate"])
        self.assertIn("Staff confirmation is required before check-in.", response.json()["warnings"])

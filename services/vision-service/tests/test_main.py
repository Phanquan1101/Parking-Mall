import base64, hashlib, hmac, json, os, time, unittest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)
secret = os.getenv("JWT_SECRET", "parkflow-local-development-secret-change-me-2026")
def token(role):
    enc=lambda x: base64.urlsafe_b64encode(json.dumps(x,separators=(",",":")).encode()).decode().rstrip("=")
    header,payload=enc({"alg":"HS384","typ":"JWT"}),enc({"sub":"test","roles":[role],"exp":time.time()+3600})
    signature=base64.urlsafe_b64encode(hmac.new(secret.encode(),f"{header}.{payload}".encode(),hashlib.sha384).digest()).decode().rstrip("=")
    return f"{header}.{payload}.{signature}"
def image(name="59A1-12345.jpg"): return {"image":(name,b"demo","image/jpeg")}
class VisionOcrTests(unittest.TestCase):
    def test_auth_and_roles(self):
        self.assertEqual(401,client.post("/api/vision/ocr/plate",files=image()).status_code)
        self.assertEqual(403,client.post("/api/vision/ocr/plate",headers={"Authorization":"Bearer "+token("MERCHANT_STAFF")},files=image()).status_code)
        self.assertEqual(200,client.post("/api/vision/ocr/plate",headers={"Authorization":"Bearer "+token("ADMIN")},files=image()).status_code)
        self.assertEqual(200,client.post("/api/vision/ocr/plate",headers={"Authorization":"Bearer "+token("PARKING_STAFF")},files=image()).status_code)
    def test_image_response_and_low_confidence_warning(self):
        headers={"Authorization":"Bearer "+token("PARKING_STAFF")}
        self.assertEqual(400,client.post("/api/vision/ocr/plate",headers=headers).status_code)
        response=client.post("/api/vision/ocr/plate",headers=headers,files=image()).json()
        self.assertTrue(response["ocrRequestId"]);self.assertEqual("59A1-12345",response["candidatePlate"]);self.assertTrue(0<=response["confidence"]<=1)
        self.assertIn("Staff confirmation",response["warnings"][0])
        low=client.post("/api/vision/ocr/plate",headers=headers,files=image("unknown.jpg")).json()
        self.assertIsNone(low["candidatePlate"]);self.assertEqual(2,len(low["warnings"]))

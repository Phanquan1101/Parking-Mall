# API_CONTRACT.md - ParkFlow Mall API Contract Draft

This is a documentation contract only. Endpoint bodies are placeholders until the relevant vertical slice is approved.

## 1. API principles

- All APIs return JSON.
- Public customer endpoints expose safe ticket data only.
- Backend enforces role-based access.
- QR Lookup Token never authorizes exit.
- Dynamic Exit Pass is short-lived and one-time use.
- Mutation retries use the HTTP `Idempotency-Key` header.
- SePay Live is disabled for MVP.

## 2. Common response formats

### Success

```json
{
  "success": true,
  "data": {},
  "meta": {}
}
```

### Error

```json
{
  "success": false,
  "error": {
    "code": "BUSINESS_RULE_VIOLATION",
    "message": "Human-readable error message",
    "details": {}
  }
}
```

Common error codes: `UNAUTHORIZED`, `FORBIDDEN`, `VALIDATION_ERROR`, `NOT_FOUND`, `BUSINESS_RULE_VIOLATION`, `TOKEN_EXPIRED`, `TOKEN_ALREADY_USED`, `PAYMENT_REQUIRED`, `PLATE_MISMATCH`, `SYNC_CONFLICT`, `DUPLICATE_REQUEST`, `MANUAL_REVIEW_REQUIRED`.

## 3. Identity

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Login and receive JWT |
| GET | `/api/auth/me` | Authenticated | Return current user and roles |

### POST `/api/auth/login`

Request:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Response:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "usr_admin",
    "username": "admin",
    "displayName": "System Admin",
    "roles": ["ADMIN"]
  }
}
```

Slice 1 uses BCrypt-hashed in-memory demo users only. Invalid credentials return `401`.

### GET `/api/auth/me`

Requires `Authorization: Bearer <accessToken>`.

Response:

```json
{
  "id": "usr_admin",
  "username": "admin",
  "displayName": "System Admin",
  "roles": ["ADMIN"]
}
```

Gateway behavior: `/api/auth/login` is public and proxied to Identity Service; `/api/auth/me` forwards the bearer token and Identity Service validates it.

## 4. Parking

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/parking/sessions/check-in` | `PARKING_STAFF`, `ADMIN` | Create an official session after staff plate confirmation |
| GET | `/api/parking/sessions/{sessionId}` | `PARKING_STAFF`, `ADMIN` | Retrieve staff session details |
| GET | `/api/parking/sessions?status=&plate=` | `PARKING_STAFF`, `ADMIN` | Search sessions for recovery/lost QR handling |
| POST | `/api/parking/sessions/{sessionId}/check-out` | `PARKING_STAFF`, `ADMIN` | Check out with valid Exit Pass and plate validation |
| POST | `/api/parking/sessions/{sessionId}/manual-override` | `PARKING_STAFF`, `ADMIN` | Authorized override requiring a reason and audit log |

Check-in and check-out mutations accept `Idempotency-Key`.

### Slice 2 implemented parking endpoints

`POST /api/parking/sessions/check-in` requires `ADMIN` or `PARKING_STAFF` JWT authentication. Slice 2 accepts `MANUAL` plate source only and returns an `ACTIVE`, `UNPAID` session with opaque `qrLookupToken` and `ticketUrl`.

```json
{
  "vehiclePlate": "59A1-12345",
  "vehicleType": "MOTORBIKE",
  "entryGate": "GATE_IN_01",
  "staffId": "staff-demo-id",
  "plateSource": "MANUAL"
}
```

`GET /api/parking/sessions/{sessionId}` and `GET /api/parking/sessions?status=&plate=` require the same roles. Slice 2 storage is in-memory only.

### Slice 5 implemented Exit Pass and check-out endpoints

`POST /api/parking/sessions/{sessionId}/exit-passes` is public for the customer ticket flow but requires `lookupToken` in the JSON body. The token must belong to the specified session. The session must be `PAID` (or have zero final fee) and must not be `EXITED`. It returns an opaque Exit Pass token, expiry, 60-second default TTL, and `ACTIVE` status. Creating a new pass invalidates an older active pass for that session.

`POST /api/parking/exit-passes/{exitPassToken}/validate` requires an `ADMIN` or `PARKING_STAFF` JWT and accepts `exitGate` and `exitPlate`. It validates but does not consume the pass. A matching normalized plate is required.

`POST /api/parking/sessions/{sessionId}/check-out` requires `ADMIN` or `PARKING_STAFF` and accepts `exitPassToken`, `exitPlate`, and `exitGate`. It consumes the valid pass, records exit details, and changes the session to `EXITED`.

`POST /api/parking/sessions/{sessionId}/manual-override` requires the same roles and accepts `reason`, `exitPlate`, and `exitGate`. It is only permitted after payment or zero fee; it records a lightweight override event and may record a suspicious plate-mismatch reason.

Exit errors return a safe `errorCode`, including `EXIT_PASS_NOT_FOUND`, `EXIT_PASS_EXPIRED`, `EXIT_PASS_ALREADY_USED`, `SESSION_NOT_PAID`, `PLATE_MISMATCH`, and `SESSION_ALREADY_EXITED`. QR Lookup Token is never accepted as `exitPassToken`.

## 5. Public ticket and Exit Pass

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/api/public/tickets/{lookupToken}` | Public lookup token | Show safe session summary, fee estimate, and payment state |
| POST | `/api/parking/sessions/{sessionId}/exit-passes` | Public lookup context, `PARKING_STAFF`, `ADMIN` | Create a pass only for `PAID` or zero-fee session |
| POST | `/api/parking/exit-passes/{exitPassToken}/validate` | `PARKING_STAFF`, `ADMIN` | Validate pass before gate decision |

### Slice 4 public ticket lookup

`GET /api/public/tickets/{lookupToken}` is public. It returns session and parking status, a temporary configurable demo flat fee, and the statement that the lookup token cannot authorize exit. It never exposes `staffId`, internal events, or token metadata. Invalid tokens return `404`.

Exit authorization requires all of the following:

- pass is valid, not expired, and not used;
- session is `PAID` or `final_fee = 0`;
- exit plate matches, or an authorized manual override with reason is recorded.

QR Lookup Token alone cannot call a check-out or gate-validation operation.

The implemented public ticket response also includes `canGenerateExitPass`, `exitPassAvailable`, and `exitPassMessage`. It never includes an active Exit Pass token; the customer must explicitly generate one after payment.

## 6. Offline synchronization

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/parking/offline-sync` | `PARKING_STAFF`, `ADMIN` | Submit queued offline events |
| GET | `/api/parking/offline-sync/{eventId}` | `PARKING_STAFF`, `ADMIN` | Retrieve synchronization result |

### Slice 6 implemented offline sync

`POST /api/parking/offline-sync` requires an `ADMIN` or `PARKING_STAFF` JWT and a nonblank `Idempotency-Key` header. It accepts a `deviceId` and events containing `eventId`, `idempotencyKey`, `localTimestamp`, `eventType`, and payload. Staff identity comes from JWT, never the client payload.

Only `OFFLINE_CHECK_IN` is fully supported. A valid event creates an official `ACTIVE` session with QR Lookup Token and returns `SYNCED`; invalid payloads return `REJECTED`; duplicate active normalized plates return `CONFLICT`; duplicate event ID/idempotency key returns `DUPLICATE` without creating another session. `GET /api/parking/offline-sync/{eventId}` requires the same role and returns the stored result or safe `404`.

Event statuses are `PENDING`, `SYNCING`, `SYNCED`, `DUPLICATE`, `REJECTED`, `CONFLICT`, and `MANUAL_REVIEW_REQUIRED`. The server result is authoritative after sync. Offline check-out is not supported and never performs automatic vehicle exit in Slice 6.

## 7. Payment

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/payments/orders` | Public ticket context, `PARKING_STAFF`, `ADMIN` | Create a payment order |
| GET | `/api/payments/orders/{paymentOrderId}` | Ticket context, `PARKING_STAFF`, `ADMIN` | Retrieve payment state |
| POST | `/api/payments/simulations/success` | `ADMIN` or approved test role | Simulate successful payment |
| POST | `/api/payments/webhooks/sepay` | Provider verification | Receive SePay Test webhook only |
| POST | `/api/payments/reconciliation/run` | `ADMIN`, scheduler, internal service | Reconcile pending/mismatched payment states |

Payment simulation, webhook, reconciliation, and order creation where retryable require `Idempotency-Key`. Payment must match `payment_code` and amount. `SEPAY_LIVE` is not an allowed MVP mode.

### Slice 4 implemented payment simulation

`POST /api/payments/orders` and `GET /api/payments/orders/{paymentOrderId}` are public only in the possession context of a valid ticket lookup token or order ID. `POST /api/payments/simulations/success` is enabled only when `PAYMENT_MODE=SIMULATION` and requires `Idempotency-Key`; retries return the previously processed order result. The simulation matches payment code and amount before updating Parking.

`POST /internal/parking/sessions/{sessionId}/payment-status` is Parking-Service-internal only, requires `X-Internal-Service-Token`, and is intentionally not routed by API Gateway. It accepts only `PAID` updates and never creates an Exit Pass.

### Slice 8 implemented reconciliation

`POST /api/payments/reconciliation/run`, `GET /api/payments/reconciliation/items`, and `GET /api/payments/reconciliation/items/{itemId}` require an `ADMIN` JWT. The run expires overdue `PENDING` orders, exposes `MISMATCHED` orders as `PENDING_MANUAL_REVIEW`, and retries PAID orders whose protected Parking update is pending reconciliation.

Item issue types include `PENDING_EXPIRED`, `PARKING_UPDATE_FAILED`, and `PAYMENT_MISMATCHED`; statuses include `OPEN`, `RESOLVED`, and `PENDING_MANUAL_REVIEW`. Reconciliation never generates Exit Passes, checks out vehicles, changes merchant discounts, refunds, or invokes SePay.

## 8. Merchant validation

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/merchant/validations` | `MERCHANT_STAFF`, `ADMIN` | Validate an invoice for an authorized tenant |
| GET | `/api/merchant/validations?sessionId=` | `MERCHANT_STAFF`, `ADMIN` | List validations for a session |
| POST | `/api/merchant/validations/{validationId}/void` | `MERCHANT_STAFF`, `ADMIN` | Void an eligible validation with audit |

`AGGREGATE_INVOICE` is the default policy. One invoice code is single-use and official validation requires online mode.

### Slice 7 implemented merchant validation

`POST /api/merchant/validations` requires `MERCHANT_STAFF` or `ADMIN` JWT, a QR Lookup Token, unique invoice code, and positive amount. The service resolves the parking ticket online, rejects exited sessions and duplicate codes, aggregates accepted invoices, then calls Parking's internal-only `POST /internal/parking/sessions/{sessionId}/discount` endpoint. The internal endpoint requires `X-Internal-Service-Token`, recalculates final fee, and is not Gateway-routed.

The demo policy is `AGGREGATE_INVOICE`: at total eligible amount `300000`, apply `5000` discount, capped at the parking estimated fee. `GET /api/merchant/validations?sessionId=` returns validation history. Official offline validation is not implemented.

## 9. Reservation

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/reservations` | Public | Create an in-memory reservation |
| GET | `/api/reservations/{reservationCode}` | Public | Retrieve reservation by opaque code |
| POST | `/api/reservations/{reservationCode}/cancel` | Public | Cancel a `RESERVED` reservation |
| GET | `/api/reservations?status=&vehiclePlate=` | `PARKING_STAFF`, `ADMIN` | List reservations |
| POST | `/api/reservations/expire` | `PARKING_STAFF`, `ADMIN` | Mark stale reservations expired |
| POST | `/internal/reservations/{reservationCode}/consume` | Internal only | Consume a matching reservation for Parking check-in |

Slice 9 statuses are `RESERVED`, `CANCELLED`, `EXPIRED`, and `CONSUMED`. Internal consume requires `X-Internal-Service-Token`, is not Gateway-routed, and rejects mismatched plate/type or reused codes. A normal parking check-in can include optional `reservationCode`; a successful consume produces an ordinary `UNPAID` parking session. Reservation payment, deposits, refunds, and reconciliation are not part of Slice 9.

## 10. OCR Assist

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/vision/ocr/plate` | `PARKING_STAFF`, `ADMIN` | Multipart image upload returning an assistive plate candidate and confidence |

`POST /api/vision/ocr/plate` accepts multipart field `image` (JPEG, PNG, or WebP) plus optional `cameraId`/`gateId`. It returns `ocrRequestId`, nullable `candidatePlate` and `normalizedCandidatePlate`, clamped `confidence` (0–1), `provider`, warnings, and `createdAt`. The route requires `ADMIN` or `PARKING_STAFF`; `MERCHANT_STAFF` is denied. Missing images and unsupported image types return `400`.

Vision selects `DEMO_OCR` or `GEMINI` only from its backend `VISION_OCR_PROVIDER` environment variable. `GEMINI` requires a server-side `GEMINI_API_KEY`; a missing/invalid provider configuration returns safe `503` detail and an unavailable configured provider returns safe `502` detail so the staff console can fall back to manual entry. No provider API key or uploaded image data is returned to the frontend. Every response includes `Staff confirmation is required before check-in.`; a missing, uncertain, or low-confidence candidate includes a manual-entry warning.

Example response:

```json
{
  "ocrRequestId": "2ffcd5a9-f6f8-4f5d-a2fd-7fd3094b75c6",
  "candidatePlate": "59A1-12345",
  "normalizedCandidatePlate": "59A112345",
  "confidence": 0.86,
  "provider": "GEMINI",
  "warnings": ["Staff confirmation is required before check-in."],
  "createdAt": "2026-07-12T00:00:00+00:00"
}
```

Parking check-in accepts optional `ocrRequestId`, `ocrCandidatePlate`, and `ocrConfidence` when `plateSource` is `OCR_ASSISTED`. `vehiclePlate` remains the confirmed staff value and source of truth. OCR is assistive only: it does not auto-create a session, bypass duplicate/reservation validation, authorize exit, or change payment/merchant/Exit Pass/check-out behavior.

Slice 10C uses these unchanged Vision and Parking contracts from the frontend `/staff/gate-entry` route. The camera page submits a transient JPEG frame only while scanning, and only one recognition request is active at a time. A successful normal check-in response already exposes `sessionCode`, `vehiclePlate`, `entryTime`, `paymentStatus`, `qrLookupToken`, and `ticketUrl`; the frontend builds the customer route as `/tickets/{qrLookupToken}`. The Lookup Ticket remains view-only and is never an Exit Pass.

### Slice 11A dashboard usage

`GET /api/parking/sessions` and `GET /api/reservations` remain `ADMIN`/`PARKING_STAFF` protected and are consumed directly by the read-only frontend dashboard. Parking session list responses include stored `reservationId`, `reservationCode`, `plateSource`, and OCR metadata. `GET /api/payments/reconciliation/items` remains `ADMIN` only; clients must gracefully handle 403 for staff users.

## 11. Reports and dashboard

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/api/reports/dashboard` | `ADMIN` | Active sessions, confirmed revenue, payment states, merchant totals, offline and suspicious states |
| GET | `/api/reports/suspicious` | `ADMIN` | List suspicious/fraud cases |
| GET | `/api/reports/offline-conflicts` | `ADMIN`, authorized staff | List sync conflicts |

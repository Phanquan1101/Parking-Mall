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

The `POST` request requires an `Idempotency-Key` header. Each event also contains `eventId`, `deviceId`, `staffId`, timestamp, event type, and payload. Results are `ACCEPTED`, `REJECTED`, `CONFLICT`, `DUPLICATE`, or `MANUAL_REVIEW_REQUIRED`.

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

## 8. Merchant validation

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/merchant/validations` | `MERCHANT_STAFF`, `ADMIN` | Validate an invoice for an authorized tenant |
| GET | `/api/merchant/validations?sessionId=` | `MERCHANT_STAFF`, `ADMIN` | List validations for a session |
| POST | `/api/merchant/validations/{validationId}/void` | `MERCHANT_STAFF`, `ADMIN` | Void an eligible validation with audit |

`AGGREGATE_INVOICE` is the default policy. One invoice code is single-use and official validation requires online mode.

## 9. Reservation

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/reservations` | Customer, `ADMIN` | Create a reservation and temporary slot hold |
| GET | `/api/reservations/{reservationId}` | Customer, `PARKING_STAFF`, `ADMIN` | Retrieve reservation state |
| POST | `/api/reservations/{reservationId}/cancel` | Customer, `ADMIN` | Cancel an eligible reservation |
| POST | `/api/reservations/{reservationId}/check-in` | `PARKING_STAFF`, `ADMIN` | Convert confirmed reservation to parking session |
| POST | `/api/reservations/reconciliation/run` | `ADMIN`, scheduler, internal service | Reconcile reservation/payment inconsistency |

Reservation supports `PENDING_RECONCILIATION`. Refund and live payment flows are out of MVP.

## 10. OCR Assist

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/vision/plate-recognition` | `PARKING_STAFF`, `ADMIN` | Return plate candidate and confidence |

OCR is assistive only. Staff confirmation or manual entry remains authoritative, and OCR failure must not block the parking flow.

## 11. Reports and dashboard

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/api/reports/dashboard` | `ADMIN` | Active sessions, confirmed revenue, payment states, merchant totals, offline and suspicious states |
| GET | `/api/reports/suspicious` | `ADMIN` | List suspicious/fraud cases |
| GET | `/api/reports/offline-conflicts` | `ADMIN`, authorized staff | List sync conflicts |

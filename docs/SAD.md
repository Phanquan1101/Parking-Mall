# System Architecture Document — SAD

## Project Name

**ParkFlow Mall — Smart Parking & Reservation Management System**

## Version

**SAD v1.0**

## Based On

* BRD v2 — ParkFlow Mall
* PRD — ParkFlow Mall
* Implementation Pack for Agent Codex
* Target architecture: Microservice-oriented system

---

# 1. Architecture Overview

## 1.1 Purpose

Tài liệu SAD này mô tả kiến trúc hệ thống ParkFlow Mall theo mô hình microservice, làm nền cho quá trình triển khai bằng Agent Codex.

SAD tập trung vào:

* Cách chia microservice.
* Trách nhiệm của từng service.
* Luồng dữ liệu chính.
* Chiến lược database và storage.
* QR security model.
* Offline mode và sync.
* Payment simulation, SePay test mode và reconciliation.
* Merchant invoice aggregation.
* Reservation workflow.
* Deployment strategy với Docker, Supabase và Railway/hybrid.
* Các tiêu chuẩn kỹ thuật cần Codex tuân thủ khi code.

SAD không thay thế BRD/PRD. BRD mô tả “vì sao làm”, PRD mô tả “cần làm gì”, còn SAD mô tả “hệ thống nên được xây như thế nào”.

---

## 1.2 Architecture Goals

| ID      | Goal                  | Description                                                                          |
| ------- | --------------------- | ------------------------------------------------------------------------------------ |
| ARC-001 | Microservice-oriented | Hệ thống được chia thành các service theo domain nghiệp vụ rõ ràng                   |
| ARC-002 | Business-rule-first   | Mọi service phải tuân thủ business rules từ BRD/PRD                                  |
| ARC-003 | Offline-resilient     | Staff Console vẫn xử lý được một số nghiệp vụ cốt lõi khi mất mạng                   |
| ARC-004 | Secure QR flow        | QR Lookup không được dùng trực tiếp để mở cổng; check-out cần Exit Pass/verification |
| ARC-005 | Payment-safe          | Payment phải idempotent, có pending state và reconciliation                          |
| ARC-006 | Demo-friendly         | Có thể chạy local bằng Docker Compose và deploy một phần lên cloud                   |
| ARC-007 | Cost-conscious        | Ưu tiên Supabase, Railway hoặc hybrid để tối ưu chi phí                              |
| ARC-008 | Agent-friendly        | Repo phải có tài liệu rõ để Codex code theo vertical slice, không tự bịa scope       |

---

## 1.3 Architecture Style

Kiến trúc đề xuất là:

```text
Microservice-oriented architecture
+ API Gateway
+ Shared PostgreSQL instance with schema-per-service for MVP
+ Service-owned schemas
+ REST APIs for synchronous communication
+ Outbox/reconciliation jobs for eventual consistency
+ Local offline queue for Staff Console
+ Dockerized local development
```

Trong production enterprise, mỗi service nên sở hữu database riêng. Tuy nhiên, với MVP chi phí thấp, dùng **một Supabase PostgreSQL project với schema riêng cho từng service** là phương án cân bằng giữa microservice boundary và chi phí vận hành.

---

## 1.4 Canonical MVP priorities

- Manual plate entry and staff confirmation are Must Have.
- OCR Assist is Should Have, assistive only, and cannot block check-in/check-out.
- Reservation Basic is Should Have, follows Payment Reconciliation, and must not block core MVP.
- Payment Simulation is Must Have; SePay Test is Should Have; SePay Live is disabled and out of MVP.
- Audit/Fraud is cross-cutting across relevant services and slices.

# 2. System Context

## 2.1 Main Actors

| Actor                | Description                                                         |
| -------------------- | ------------------------------------------------------------------- |
| Customer             | Khách gửi xe, quét QR, xem phí, thanh toán, nhận Exit Pass          |
| Parking Staff        | Nhân viên cổng vào/ra, check-in, check-out, xử lý ngoại lệ          |
| Merchant Staff       | Nhân viên cửa hàng, xác nhận hóa đơn giảm phí                       |
| Admin / Mall Manager | Quản lý bãi xe, doanh thu, dashboard, cảnh báo                      |
| Payment Provider     | SePay hoặc payment simulation service                               |
| OCR Service          | Service nhận diện biển số từ ảnh/camera                             |
| System Scheduler     | Job nội bộ xử lý reconciliation, expired reservation, sync conflict |

---

## 2.2 External Dependencies

| Dependency                        | Purpose                   | MVP Strategy                                           |
| --------------------------------- | ------------------------- | ------------------------------------------------------ |
| Supabase PostgreSQL               | Database managed          | Dùng schema-per-service                                |
| Supabase Storage                  | Lưu ảnh xe, ảnh biển số   | Dùng bucket riêng, signed URL hoặc backend proxy       |
| SePay                             | Payment webhook/VietQR    | Không dùng live trong MVP; dùng Simulation + Test Mode |
| Railway                           | Deploy cloud chi phí thấp | Dùng cho Gateway/Core/Payment nếu ổn                   |
| Docker Compose                    | Local development         | Bắt buộc cho multi-service                             |
| OCR GitHub Repo                   | Nhận diện biển số         | Đóng gói thành Vision Service                          |
| Browser Local Storage / IndexedDB | Offline queue/cache       | Dùng cho Staff Console offline mode                    |

---

# 3. High-Level Architecture

## 3.1 Logical Architecture

```text
                 ┌────────────────────┐
                 │    React Frontend   │
                 │ Customer / Staff /  │
                 │ Merchant / Admin    │
                 └─────────┬──────────┘
                           │
                           ▼
                 ┌────────────────────┐
                 │    API Gateway      │
                 │ Spring Cloud Gateway│
                 └─────────┬──────────┘
                           │
       ┌───────────────────┼───────────────────┐
       │                   │                   │
       ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌────────────────┐
│ Identity     │   │ Parking      │   │ Payment        │
│ Service      │   │ Service      │   │ Service        │
└──────────────┘   └──────┬───────┘   └──────┬─────────┘
                          │                  │
                          ▼                  ▼
                  ┌──────────────┐   ┌────────────────┐
                  │ Reservation  │   │ Merchant       │
                  │ Service      │   │ Validation     │
                  └──────────────┘   └────────────────┘
                          │
                          ▼
                  ┌──────────────┐
                  │ Reporting    │
                  │ Service      │
                  └──────────────┘

                  ┌──────────────┐
                  │ Vision/OCR   │
                  │ Python API   │
                  └──────────────┘

                           │
                           ▼
       ┌────────────────────────────────────────┐
       │ Supabase PostgreSQL + Supabase Storage │
       └────────────────────────────────────────┘
```

---

## 3.2 Runtime Architecture

Trong MVP, hệ thống có thể chạy theo 2 chế độ:

### Mode A — Local Development

```text
Laptop
├── Docker Compose
│   ├── api-gateway
│   ├── identity-service
│   ├── parking-service
│   ├── payment-service
│   ├── merchant-service
│   ├── reservation-service
│   ├── reporting-service
│   ├── vision-service
│   └── frontend
└── Supabase remote hoặc PostgreSQL local
```

### Mode B — Demo Hybrid

```text
Cloud
├── Frontend
├── API Gateway
├── Core backend services
├── Payment webhook endpoint
└── Supabase DB/Storage

Laptop
└── Vision/OCR service hoặc full fallback local
```

Phương án hybrid được khuyến nghị cho demo vì OCR có thể nặng và free-tier cloud có thể cold start.

---

# 4. Microservice Decomposition

## 4.1 Service List

| Service                     | Responsibility                                                                     | Tech                 |
| --------------------------- | ---------------------------------------------------------------------------------- | -------------------- |
| API Gateway                 | Routing, auth filter, CORS, rate limit cơ bản                                      | Spring Cloud Gateway |
| Identity Service            | User, role, JWT, auth                                                              | Spring Boot          |
| Parking Service             | Parking session, check-in/out, QR Lookup, Exit Pass, offline sync                  | Spring Boot          |
| Payment Service             | Payment order, simulation webhook, SePay test webhook, idempotency, reconciliation | Spring Boot          |
| Merchant Validation Service | Merchant, invoice validation, invoice aggregation, discount rule                   | Spring Boot          |
| Reservation Service         | Reservation, slot hold, payment state, no-show/expired                             | Spring Boot          |
| Reporting Service           | Dashboard, operational metrics, pending/suspicious view                            | Spring Boot          |
| Vision/OCR Service          | License plate detection/OCR                                                        | Python FastAPI       |
| Frontend                    | Staff, Customer, Merchant, Admin UI                                                | React + Vite         |

---

## 4.2 API Gateway

### Responsibilities

* Route API requests to downstream services.
* Validate JWT for protected endpoints.
* Allow public access to customer ticket lookup and payment redirect endpoints.
* Apply basic CORS policy.
* Apply basic rate limiting for QR lookup/payment endpoints if implemented.
* Hide internal service URLs from frontend.

### Route Draft

| Path Prefix              | Target Service              | Auth                  |
| ------------------------ | --------------------------- | --------------------- |
| `/api/auth/**`           | Identity Service            | Public/Protected      |
| `/api/parking/**`        | Parking Service             | Mixed                 |
| `/api/payments/**`       | Payment Service             | Mixed                 |
| `/api/merchant/**`       | Merchant Validation Service | Merchant/Admin        |
| `/api/reservations/**`   | Reservation Service         | Mixed                 |
| `/api/reports/**`        | Reporting Service           | Admin                 |
| `/api/vision/**`         | Vision/OCR Service          | Staff/Admin           |
| `/api/public/tickets/**` | Parking Service             | Public token          |
| `/api/payments/webhooks/sepay` | Payment Service             | Provider verification |

---

## 4.3 Identity Service

### Responsibilities

* User login/logout.
* JWT issuing.
* Role management.
* User status.
* Password hashing.
* Basic user profile.

### Roles

```text
ADMIN
PARKING_STAFF
MERCHANT_STAFF
CUSTOMER_OPTIONAL
```

MVP không bắt buộc customer đăng ký. Customer có thể truy cập ticket bằng token.

### Owned Schema

```text
identity_schema
```

### Core Tables

```text
users
roles
user_roles
refresh_tokens_optional
```

---

## 4.4 Parking Service

### Responsibilities

* Create parking session.
* Manage check-in/check-out.
* Generate QR Lookup Token.
* Generate Dynamic Exit Pass.
* Validate plate match.
* Manage session status.
* Manage offline event sync.
* Create suspicious alerts.
* Record audit-relevant parking actions.

### Core Statuses

```text
ACTIVE
PENDING_PAYMENT
PAID
EXITED
SUSPICIOUS
LOST_QR
OFFLINE_PENDING_SYNC
CONFLICT
CANCELLED
```

### Owned Schema

```text
parking_schema
```

### Core Tables

```text
parking_sessions
qr_lookup_tokens
exit_passes
vehicles
gates
parking_zones
offline_events
sync_conflicts
fraud_alerts
parking_audit_logs
```

### Important Business Rules

* QR Lookup Token cannot open gate.
* Exit Pass has TTL and is one-time use.
* Plate mismatch triggers SUSPICIOUS.
* Offline events must be idempotent.
* Server is final source of truth after sync.

---

## 4.5 Payment Service

### Responsibilities

* Create payment orders.
* Support Payment Simulation Mode.
* Support SePay Test Mode.
* Receive webhook events.
* Enforce idempotency.
* Match payment amount and payment code.
* Update payment status.
* Trigger payment reconciliation.
* Expose payment state to Parking/Reservation.

### Payment Modes

```text
SIMULATION
SEPAY_TEST
SEPAY_LIVE (disabled, future only)
```

MVP default: `SIMULATION`.

SePay Live is out of MVP.

### Owned Schema

```text
payment_schema
```

### Core Tables

```text
payment_orders
payment_transactions
webhook_events
payment_reconciliation_jobs
payment_reconciliation_items
```

### Payment Status

```text
PENDING
PAID
FAILED
EXPIRED
MISMATCHED
PENDING_RECONCILIATION
```

### Key Design Rule

Payment must be idempotent. Duplicate webhook/simulation calls must not create duplicate payment records.

---

## 4.6 Merchant Validation Service

### Responsibilities

* Manage merchants/tenants.
* Allow merchant staff to validate invoices.
* Support invoice aggregation.
* Prevent invoice reuse.
* Apply discount rule.
* Send discount result to Parking Service.
* Record merchant validation audit.

### Owned Schema

```text
merchant_schema
```

### Core Tables

```text
tenants
merchant_staff_profiles
invoice_validations
discount_rules
discount_applications
merchant_audit_logs
```

### Invoice Policy

Default MVP policy:

```text
AGGREGATE_INVOICE
```

Meaning:

* One parking session may have multiple invoices.
* Invoices from multiple merchants can be accumulated.
* Total eligible invoice amount determines discount.
* One invoice code can only be used once.

Alternative policy:

```text
SINGLE_INVOICE
```

Can be supported later as configuration.

---

## 4.7 Reservation Service

### Responsibilities

* Create reservation.
* Hold slot temporarily.
* Track reservation payment state.
* Confirm reservation after payment.
* Expire unpaid reservation.
* Mark no-show.
* Convert reservation to parking session on valid check-in.
* Reconcile payment/reservation inconsistencies.

### Owned Schema

```text
reservation_schema
```

### Core Tables

```text
reservations
reservation_slot_holds
reservation_policies
reservation_reconciliation_items
```

### Reservation Status

```text
DRAFT
PENDING_PAYMENT
CONFIRMED
CHECKED_IN
EXPIRED
NO_SHOW
CANCELLED
PENDING_RECONCILIATION
```

### Distributed Transaction Strategy

Reservation uses a Saga-like flow:

```text
1. Create reservation request
2. Hold slot temporarily
3. Create payment order
4. Wait for payment success
5. Confirm reservation if slot hold is still valid
6. If payment fails/timeout, release slot
7. If payment succeeds but confirmation fails, mark PENDING_RECONCILIATION
```

---

## 4.8 Reporting Service

### Responsibilities

* Provide dashboard metrics.
* Aggregate operational data.
* Show active sessions.
* Show revenue summary.
* Show payment pending.
* Show merchant validation summary.
* Show offline pending sync.
* Show suspicious sessions.
* Show sync conflicts.

### MVP Strategy

Reporting Service can query read models or service APIs. For MVP, it may read from service schemas with read-only database credentials if implementation complexity needs to be reduced.

### Owned Schema

```text
reporting_schema
```

### Optional Tables

```text
dashboard_snapshots
reporting_read_models
```

---

## 4.9 Vision/OCR Service

### Responsibilities

* Receive image upload or frame.
* Detect license plate area.
* OCR plate characters.
* Return plate candidate and confidence.
* Never make final business decision.

### Tech

```text
Python
FastAPI
OpenCV
YOLOv8 / selected GitHub repo
EasyOCR / PaddleOCR optional
```

### API Draft

```http
POST /api/vision/plate-recognition
```

Response:

```json
{
  "plate": "59A1-12345",
  "confidence": 0.86,
  "bbox": [100, 50, 220, 90],
  "normalizedPlate": "59A112345",
  "processingTimeMs": 1420
}
```

### Rule

OCR is assistive. Staff must confirm or correct plate if confidence is low.

---

# 5. Data Architecture

## 5.1 Database Strategy

MVP uses:

```text
One Supabase PostgreSQL project
+ multiple schemas
+ service-owned tables
```

Schema draft:

```text
identity_schema
parking_schema
payment_schema
merchant_schema
reservation_schema
reporting_schema
```

Rationale:

* Lower cost.
* Easier setup.
* Still preserves logical ownership.
* Easier for student/demo project.
* Can migrate to database-per-service later.

---

## 5.2 Storage Strategy

Use Supabase Storage or equivalent object storage.

Buckets:

```text
vehicle-entry-images
vehicle-exit-images
plate-crops
invoice-images-optional
```

Rules:

* Images are not public by default.
* Frontend should not access raw public URLs unless signed.
* Staff/Admin can view images through authorized backend endpoint or signed URL.
* Customer Ticket Page should show minimal data only.

---

## 5.3 Data Ownership

| Data                         | Owner Service                                                 |
| ---------------------------- | ------------------------------------------------------------- |
| Users, roles                 | Identity Service                                              |
| Parking sessions             | Parking Service                                               |
| QR tokens, Exit Passes       | Parking Service                                               |
| Offline events               | Parking Service                                               |
| Payment orders, transactions | Payment Service                                               |
| Invoice validations          | Merchant Validation Service                                   |
| Reservations                 | Reservation Service                                           |
| Dashboard views              | Reporting Service                                             |
| OCR result metadata          | Parking Service or Vision Service depending on implementation |

---

## 5.4 Cross-Service Data Rule

Services should not directly modify another service’s schema.

Allowed patterns:

1. Synchronous API call.
2. Event/outbox table.
3. Reconciliation job.
4. Read-only reporting query for MVP only.

---

# 6. Core Business Flows

## 6.1 Normal Check-in Flow

```text
Parking Staff opens Staff Console
→ Captures/uploads plate image
→ Vision Service returns plate suggestion
→ Staff confirms/corrects plate
→ Parking Service creates parking session
→ Parking Service creates QR Lookup Token
→ Staff gives/displays QR to customer
→ Session status = ACTIVE
```

---

## 6.2 Customer Ticket Lookup Flow

```text
Customer scans QR Lookup Token
→ Customer Ticket Page opens
→ Parking Service validates token
→ System displays session summary, duration, fee estimate, discount, payment status
```

Important:

```text
QR Lookup Token does not authorize check-out.
```

---

## 6.3 Payment Simulation Flow

```text
Customer clicks Pay
→ Payment Service creates payment order
→ Frontend displays simulated payment screen
→ Tester triggers simulation webhook
→ Payment Service validates payment code and amount
→ Payment status = PAID
→ Parking Service updates session payment status
→ Parking Service creates Dynamic Exit Pass
```

---

## 6.4 SePay Test Flow

```text
Customer clicks Pay
→ Payment Service creates order with payment_code
→ System generates VietQR/test payment info
→ SePay sends webhook to public endpoint
→ Payment Service stores webhook event
→ Payment Service enforces idempotency
→ Payment Service matches amount/payment_code
→ Payment marked PAID or MISMATCHED
→ Parking/Reservation updated or marked pending reconciliation
```

Live mode is not allowed in MVP unless explicitly approved.

---

## 6.5 Dynamic Exit Pass Flow

```text
Payment becomes PAID
→ Parking Service creates Exit Pass
→ Exit Pass has short TTL
→ Customer presents Exit Pass at exit
→ Staff scans Exit Pass
→ Parking Service validates:
   - pass exists
   - not expired
   - not used
   - session is PAID or final_fee = 0
   - plate matches
→ Check-out allowed
→ Exit Pass marked used
→ Session status = EXITED
```

---

## 6.6 Merchant Invoice Aggregation Flow

```text
Customer buys items in mall
→ Merchant Staff scans QR Lookup
→ Merchant enters invoice_code and invoice_amount
→ Merchant Validation Service checks:
   - invoice not used
   - merchant staff belongs to tenant
   - session is active/not exited
→ Invoice is added to session aggregation
→ Total eligible invoice amount recalculated
→ Discount rule applied
→ Parking fee updated
```

Default policy:

```text
Multiple invoices may be aggregated for one session.
```

---

## 6.7 Offline Check-in Flow

```text
Staff Console detects offline
→ Staff captures plate and image if possible
→ Frontend creates local temporary session
→ Offline event is stored in local queue
→ Local QR/offline reference can be shown
→ When online returns, event syncs to Parking Service
→ Server creates official session or marks conflict
```

---

## 6.8 Offline Check-out Flow

```text
Staff Console detects offline
→ Staff searches local cached active sessions by plate/QR/session code
→ If local session has payment proof/final_fee = 0/valid offline proof:
   - Staff may create offline exit event
   - Manual reason required if evidence incomplete
→ Event stored in offline queue
→ When online returns, server syncs event
→ Server accepts/rejects/conflicts event
```

Offline check-out must be conservative. If evidence is insufficient, the system should force manual review rather than silently allowing a risky exit.

---

## 6.9 Reservation Flow

```text
Customer creates reservation
→ Reservation Service holds slot temporarily
→ Payment Service creates payment order
→ Payment success event arrives
→ Reservation Service confirms if hold is valid
→ Customer receives reservation QR
→ At check-in time, staff scans QR or plate
→ Reservation converted to parking session
```

If payment succeeds but reservation confirmation fails:

```text
Reservation status = PENDING_RECONCILIATION
Payment status = PAID
Reconciliation job resolves or flags manual review
```

---

# 7. Offline Architecture

## 7.1 Offline Principle

Offline mode is not a full replacement for online system. It is a controlled degraded mode.

Allowed offline operations:

| Operation            | Offline Support          | Notes                                 |
| -------------------- | ------------------------ | ------------------------------------- |
| Check-in             | Yes                      | Creates offline event                 |
| Check-out            | Limited                  | Requires local evidence/manual reason |
| Merchant validation  | No official confirmation | Optional draft only                   |
| Payment              | No real confirmation     | Customer proof/manual review only     |
| Dashboard            | Limited                  | Shows last known data                 |
| Reservation creation | No                       | Requires online payment/slot state    |
| Reservation check-in | Limited                  | Only if cached reservation exists     |

---

## 7.2 Local Cache

Staff Console may cache:

```text
- active session summary
- plate
- session code
- payment status
- final fee
- grace period until
- QR lookup reference
- last sync timestamp
```

Staff Console should not cache full sensitive data longer than needed.

---

## 7.3 Offline Event Structure

```json
{
  "eventId": "uuid",
  "eventType": "OFFLINE_CHECK_IN",
  "deviceId": "staff-device-001",
  "staffId": "user-id",
  "localTimestamp": "2026-07-11T10:00:00+07:00",
  "payload": {},
  "idempotencyKey": "uuid",
  "syncStatus": "PENDING"
}
```

---

## 7.4 Sync Status

```text
PENDING
SYNCING
SYNCED
REJECTED
CONFLICT
```

---

## 7.5 Conflict Handling

Conflict examples:

| Conflict                                                 | Handling                          |
| -------------------------------------------------------- | --------------------------------- |
| Offline check-in plate already has active session online | Mark conflict; admin/staff review |
| Offline check-out for session already exited             | Reject duplicate                  |
| Offline exit without payment                             | Manual review                     |
| Offline timestamp older than official state transition   | Conflict                          |
| Plate mismatch during sync                               | Suspicious                        |

---

# 8. QR Security Architecture

## 8.1 Token Types

| Token Type          | Purpose                                | Can Open Gate?         |
| ------------------- | -------------------------------------- | ---------------------- |
| QR Lookup Token     | Open ticket page / find session        | No                     |
| Dynamic Exit Pass   | Authorize exit after payment           | Yes, with validation   |
| Offline Proof Token | Optional signed proof for offline mode | Limited                |
| Reservation QR      | Identify reservation                   | No direct gate opening |

---

## 8.2 QR Lookup Token

Properties:

```text
- opaque random token
- not sequential
- not predictable
- maps to one parking session
- does not contain plate or payment amount
- can be revoked
```

---

## 8.3 Dynamic Exit Pass

Properties:

```text
- generated only after payment success or final_fee = 0
- short TTL
- one-time use
- linked to session
- validated with plate match
- cannot be reused after EXITED
```

Suggested TTL for MVP:

```text
60 seconds
```

If expired, customer can refresh while online.

---

## 8.4 Screenshot Policy

A screenshot of QR Lookup Token:

```text
- may help staff find the session
- must not be enough to authorize exit
- still requires payment/plate/Exit Pass verification
```

---

## 8.5 Device Claim Optional

Device claim can be added after MVP if needed:

```text
First browser that opens QR ticket receives device-bound claim token.
Exit Pass generation may require same claim token.
If customer changes device, staff recovery is required.
```

This improves security but may increase UX complexity.

---

# 9. Payment & Reconciliation Architecture

## 9.1 Payment Modes

| Mode       | Usage                 |
| ---------- | --------------------- |
| SIMULATION | Default MVP mode      |
| SEPAY_TEST | Integration test mode |
| SEPAY_LIVE | Future, not MVP       |

---

## 9.2 Idempotency

Payment Service must enforce:

```text
- unique payment_order_id
- unique payment_code
- unique provider_transaction_id when available
- unique webhook_event_id or payload hash
```

Duplicate webhook/simulation calls must be safely ignored.

---

## 9.3 Payment State Machine

```text
PENDING
→ PAID
→ MISMATCHED
→ FAILED
→ EXPIRED
→ PENDING_RECONCILIATION
```

---

## 9.4 Reconciliation Job

The reconciliation job checks:

```text
- payment_orders stuck in PENDING
- webhook_events received but not applied
- reservations paid but not confirmed
- sessions paid but parking status not updated
- duplicate/mismatched payment records
```

Recommended MVP interval:

```text
Every 1–5 minutes in demo/test environment
```

---

## 9.5 Payment Failure Handling

| Scenario                                     | Handling                             |
| -------------------------------------------- | ------------------------------------ |
| Webhook duplicate                            | Ignore duplicate, return success     |
| Amount too low                               | MISMATCHED, manual review            |
| Payment code missing                         | PENDING_RECONCILIATION               |
| Payment success but Parking Service down     | Payment PAID, session update pending |
| Payment success but Reservation Service down | Reservation PENDING_RECONCILIATION   |
| Late payment after reservation expired       | Manual review / future refund policy |

---

# 10. Reservation Architecture

## 10.1 Reservation State Machine

```text
DRAFT
→ PENDING_PAYMENT
→ CONFIRMED
→ CHECKED_IN
→ EXPIRED
→ NO_SHOW
→ CANCELLED
→ PENDING_RECONCILIATION
```

---

## 10.2 Slot Hold

Slot hold is temporary.

Suggested MVP rule:

```text
Hold duration = 10 minutes
```

If customer does not complete payment within hold duration:

```text
reservation = EXPIRED
slot hold = RELEASED
payment order = EXPIRED
```

---

## 10.3 Saga-Like Workflow

```text
1. Create reservation
2. Hold slot
3. Create payment order
4. Payment success event
5. Confirm reservation
6. On failure, mark PENDING_RECONCILIATION
7. Reconciliation job resolves
```

---

# 11. Merchant Validation Architecture

## 11.1 Invoice Aggregation

Default MVP policy:

```text
AGGREGATE_INVOICE
```

Example:

```text
Invoice A: 120,000 VND
Invoice B: 180,000 VND
Total eligible = 300,000 VND
Discount applied
```

---

## 11.2 Validation Rules

```text
- One invoice can only be used once.
- Merchant staff can only validate invoices for their tenant.
- Session must not be EXITED.
- Discount cannot exceed parking fee.
- Merchant validation requires online mode for official confirmation.
```

---

## 11.3 Discount Application

Discount Service may remain part of Merchant Validation Service in MVP.

Discount result sent to Parking Service:

```json
{
  "sessionId": "PS-001",
  "totalInvoiceAmount": 300000,
  "discountType": "FREE_MINUTES",
  "freeMinutes": 120,
  "discountAmount": 10000
}
```

---

# 12. API Contract Strategy

## 12.1 API Style

Use REST APIs for MVP.

Conventions:

```text
- JSON request/response
- JWT for protected endpoints
- Opaque token for public ticket lookup
- Idempotency-Key header for payment/offline sync
- Consistent error response
```

---

## 12.2 Error Response Format

```json
{
  "errorCode": "PAYMENT_NOT_COMPLETED",
  "message": "Session is not paid yet.",
  "details": {},
  "traceId": "abc-123"
}
```

---

## 12.3 Core API Groups

```text
/api/auth/login
/api/auth/me
/api/parking/sessions/check-in
/api/parking/sessions/{sessionId}
/api/parking/sessions?status=&plate=
/api/parking/sessions/{sessionId}/check-out
/api/parking/sessions/{sessionId}/manual-override
/api/public/tickets/{lookupToken}
/api/parking/sessions/{sessionId}/exit-passes
/api/parking/exit-passes/{exitPassToken}/validate
/api/parking/offline-sync
/api/parking/offline-sync/{eventId}
/api/payments/orders
/api/payments/orders/{paymentOrderId}
/api/payments/simulations/success
/api/payments/webhooks/sepay
/api/payments/reconciliation/run
/api/merchant/validations
/api/merchant/validations?sessionId=
/api/merchant/validations/{validationId}/void
/api/reservations
/api/reservations/{reservationId}
/api/reservations/{reservationId}/cancel
/api/reservations/{reservationId}/check-in
/api/reservations/reconciliation/run
/api/reports/dashboard
/api/reports/suspicious
/api/reports/offline-conflicts
/api/vision/plate-recognition
```

---

# 13. Security Architecture

## 13.1 Authentication

Protected roles use JWT:

```text
ADMIN
PARKING_STAFF
MERCHANT_STAFF
```

Customer public ticket flow uses token-based access.

---

## 13.2 Authorization

Backend must enforce permissions. Frontend hiding buttons is not enough.

Examples:

| Action                     | Required Role             |
| -------------------------- | ------------------------- |
| Create staff user          | ADMIN                     |
| Check-in vehicle           | PARKING_STAFF             |
| Check-out vehicle          | PARKING_STAFF             |
| Validate merchant invoice  | MERCHANT_STAFF            |
| View dashboard             | ADMIN                     |
| Trigger payment simulation | ADMIN or system/test role |
| View vehicle image         | ADMIN or PARKING_STAFF    |

---

## 13.3 Sensitive Data

Sensitive data:

```text
- vehicle plate
- vehicle images
- payment transaction data
- staff identity
- offline cache
```

Protection:

```text
- role-based access
- signed URL or backend proxy for images
- token entropy
- no raw data in QR
- cache TTL
- audit log
```

---

# 14. Observability & Audit

## 14.1 Audit Events

Must audit:

```text
- staff login
- check-in
- check-out
- plate edit
- payment update
- webhook processing
- merchant validation
- invoice duplicate attempt
- manual override
- offline event creation
- offline sync result
- sync conflict
- reservation confirmation/failure
```

---

## 14.2 Logging

Each service should log:

```text
- traceId
- requestId
- actorId when available
- service name
- operation
- success/failure
- errorCode
```

---

## 14.3 Dashboard Pending Indicators

Dashboard must distinguish:

```text
CONFIRMED
PENDING_PAYMENT
PENDING_SYNC
PENDING_RECONCILIATION
SUSPICIOUS
CONFLICT
```

Admin must not assume all displayed data is final.

---

# 15. Deployment Architecture

## 15.1 Local Development

Use Docker Compose.

Services:

```text
frontend
api-gateway
identity-service
parking-service
payment-service
merchant-service
reservation-service
reporting-service
vision-service
```

Optional:

```text
postgres-local
minio-local
```

If using Supabase remote, local Postgres may not be needed.

---

## 15.2 Cloud / Hybrid Demo

Recommended:

```text
Railway:
- api-gateway
- identity-service
- parking-service
- payment-service
- frontend optional

Supabase:
- PostgreSQL
- Storage

Local laptop:
- vision-service optional
- full fallback compose
```

---

## 15.3 Environment Variables

Each service must use `.env` or deployment env vars.

Examples:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
SUPABASE_URL
SUPABASE_SERVICE_ROLE_KEY
SUPABASE_STORAGE_BUCKET
PAYMENT_MODE=SIMULATION
SEPAY_WEBHOOK_SECRET
EXIT_PASS_TTL_SECONDS=60
GRACE_PERIOD_MINUTES=15
OFFLINE_SYNC_ENABLED=true
```

---

# 16. Development Strategy for Agent Codex

## 16.1 Required Repo Files

Before coding, repo must contain:

```text
AGENTS.md
README.md

/docs/BRD.md
/docs/PRD.md
/docs/SAD.md
/docs/BUSINESS_RULES.md
/docs/API_CONTRACT.md
/docs/DATABASE_SCHEMA.md
/docs/TEST_MATRIX.md
/docs/DECISION_LOG.md

/agent/PLAN.md
/agent/TASKS.md
/agent/CODING_RULES.md
/agent/DEFINITION_OF_DONE.md
/agent/VERTICAL_SLICE_ROADMAP.md
/agent/CODEX_PROMPT_TEMPLATE.md
```

---

## 16.2 Coding Order

Codex must implement by vertical slice:

```text
Slice 0 — Repository Foundation
Slice 1 — API Gateway + Identity Skeleton
Slice 2 — Parking Session + QR Lookup
Slice 3 — Customer Ticket Page
Slice 4 — Payment Simulation
Slice 5 — Dynamic Exit Pass + Check-out
Slice 6 — Offline Staff Mode
Slice 7 — Merchant Invoice Aggregation
Slice 8 — Payment Reconciliation
Slice 9 — Reservation Basic
Slice 10 — OCR Assist
Slice 11 — Dashboard
Slice 12 — Docker + Deploy Hardening
```

---

## 16.3 Agent Coding Rules

Codex must not:

```text
- implement SePay Live in MVP
- use QR Lookup Token as gate authorization
- skip payment idempotency
- skip audit log for sensitive actions
- make OCR mandatory for check-in
- allow merchant validation official confirmation offline
- silently ignore sync conflicts
- add unapproved features
```

Codex must:

```text
- read AGENTS.md first
- follow TASKS.md
- update DECISION_LOG.md when assumptions are made
- update API_CONTRACT.md when APIs change
- add or update tests for each slice
- report what was changed and how it was tested
```

---

# 17. Architecture Risks

| Risk                          | Impact              | Mitigation                                      |
| ----------------------------- | ------------------- | ----------------------------------------------- |
| Microservice too complex      | Slow progress       | Build vertical slices, not all services at once |
| Railway/free tier instability | Demo failure        | Keep local Docker fallback                      |
| OCR poor accuracy             | Bad UX              | OCR assist only, manual correction              |
| Payment mismatch              | Wrong gate decision | Idempotency + reconciliation                    |
| Offline conflict              | Data inconsistency  | Event IDs + conflict queue                      |
| QR misuse                     | Security issue      | Lookup QR + Exit Pass                           |
| Invoice fraud                 | Revenue loss        | One invoice one use + audit                     |
| Supabase connection limit     | Runtime errors      | Connection pooling / fewer services in demo     |

---

# 18. Architecture Decision Records

Initial ADRs:

| ADR ID  | Decision                                                  | Status   |
| ------- | --------------------------------------------------------- | -------- |
| ADR-001 | Use microservice-oriented architecture                    | Accepted |
| ADR-002 | Use schema-per-service on one Supabase PostgreSQL for MVP | Accepted |
| ADR-003 | Use Payment Simulation before SePay Test/Live             | Accepted |
| ADR-004 | Do not use QR Lookup Token for gate authorization         | Accepted |
| ADR-005 | Use Dynamic Exit Pass for check-out                       | Accepted |
| ADR-006 | Support basic offline staff mode                          | Accepted |
| ADR-007 | Merchant validation uses invoice aggregation by default   | Accepted |
| ADR-008 | OCR is assistive, not authoritative                       | Accepted |
| ADR-009 | Deploy with Docker + Railway/Supabase/hybrid              | Accepted |
| ADR-010 | Reservation uses Saga-like flow and reconciliation        | Accepted |

---

# 19. Definition of Architecture Done

SAD is considered ready for implementation when:

| Item                                              | Status |
| ------------------------------------------------- | ------ |
| Service boundaries are defined                    | Done   |
| Data ownership is defined                         | Done   |
| QR security model is defined                      | Done   |
| Offline mode is defined                           | Done   |
| Payment simulation and reconciliation are defined | Done   |
| Merchant invoice aggregation is defined           | Done   |
| Reservation saga is defined                       | Done   |
| Deployment strategy is defined                    | Done   |
| Codex vertical slice order is defined             | Done   |
| Required repo docs are defined                    | Done   |

---

# 20. Next Step

After this SAD, the next step is not to ask Codex to code the whole project.

The next correct step is:

```text
Prepare repo
→ place BRD, PRD, SAD and Implementation Pack in repo
→ verify AGENTS.md references all docs correctly
→ give Codex Slice 0 only
→ review generated skeleton
→ continue slice by slice
```

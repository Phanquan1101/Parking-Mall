# PLAN.md — ParkFlow Mall Implementation Plan

## 1. Implementation philosophy

ParkFlow Mall must be implemented by vertical slice, not by building all services independently first.

A vertical slice means one business workflow works end-to-end across frontend, backend, database, and tests.

## 2. Architecture approach

Use a microservice-oriented architecture while keeping MVP complexity controlled.

### Planned services

| Service | Responsibility | MVP status |
|---|---|---|
| API Gateway | Request routing, CORS, auth forwarding, gateway-level rate limiting | Required |
| Identity Service | Users, roles, JWT, staff/merchant/admin access | Required |
| Parking Service | Parking sessions, QR Lookup, Exit Pass, check-in/out, offline sync, fraud alerts | Required |
| Payment Service | Payment orders, Simulation Mode, SePay Test Mode, webhook idempotency, reconciliation | Required |
| Merchant Service | Merchant accounts, invoice validation, invoice aggregation, discount application | Required |
| Reservation Service | Reservation, slot hold, payment state, expiry/no-show | Should Have after core flow |
| Vision Service | OCR plate recognition from image/camera frame | Should Have after core flow |
| Reporting Service | Dashboard aggregations | Can start inside Parking Service, split later |
| Web App | React frontend for Admin, Staff, Merchant, Customer | Required |

## 3. Data ownership rule

For MVP cost control, use one Supabase PostgreSQL project. Prefer schema separation by service:

```text
identity_schema
parking_schema
payment_schema
merchant_schema
reservation_schema
reporting_schema
```

Service boundaries should be respected in application code even if the database project is shared.

## 4. Payment strategy

Payment must be implemented in three phases:

1. `SIMULATION_MODE` — local/test endpoint simulates webhook success.
2. `SEPAY_TEST_MODE` — receive SePay test webhook or test transaction flow.
3. `LIVE_MODE` — out of scope for MVP unless explicitly approved.

Default MVP payment mode is `SIMULATION_MODE`.

## 5. Offline strategy

Offline is a business requirement, not a technical afterthought.

Minimum MVP Offline Staff Mode:

- Staff Console shows Online / Offline / Syncing status.
- Active session cache is available locally for recent sessions.
- Offline check-in creates a temporary local event.
- Offline check-out requires local evidence or manual override reason.
- Offline events are queued with idempotency keys.
- Events sync when online.
- Server accepts, rejects, or marks conflicts.
- Dashboard shows pending sync and conflict states.

## 6. QR security strategy

Do not use one static QR for everything.

- QR Lookup Token: opens ticket page only.
- Dynamic Exit Pass: generated after valid payment or zero-fee state; short TTL; one-time use.
- Screenshot of QR Lookup must not authorize automatic exit.
- Staff can always recover by plate lookup + image verification + manual override with audit.

## 7. Reservation strategy

Reservation is important but must come after parking core and payment simulation.

Reservation must include:

- Slot hold window
- Payment pending state
- Confirmed state only after payment success and valid slot hold
- Expired/no-show state
- Reconciliation for payment success but reservation update failure

## 8. Recommended phase order

| Phase | Goal | Outcome |
|---|---|---|
| Phase 0 | Repository Foundation | Folders, docs, env, Docker placeholder; no business logic |
| Phase 1 | API Gateway + Identity Skeleton | Gateway/identity skeleton, login placeholder, roles, health checks |
| Phase 2 | Parking Session + QR Lookup | Check-in draft, lookup token, public lookup; no exit authorization |
| Phase 3 | Customer Ticket Page | Ticket summary, fee estimate, payment status; no payment logic |
| Phase 4 | Payment Simulation | Payment order, simulation success, Idempotency-Key, payment status update |
| Phase 5 | Dynamic Exit Pass + Check-out | One-time pass, plate match, grace period, secure check-out |
| Phase 6 | Offline Staff Mode | Local cache/queue, sync contract, conflict state |
| Phase 7 | Merchant Invoice Aggregation | Aggregate invoices, duplicate rejection, discount recalculation |
| Phase 8 | Payment Reconciliation | Pending/mismatched payment and session consistency |
| Phase 9 | Reservation Basic | Slot hold, payment state, check-in window, reconciliation |
| Phase 10 | OCR Assist | Vision contract, candidate/confidence, staff confirmation/correction |
| Phase 11 | Dashboard | Confirmed revenue, pending/mismatched, sync, suspicious/conflict states |
| Phase 12 | Docker + Deploy Hardening | Docker completion, Railway/Supabase/hybrid docs, demo readiness |

## 9. Stop conditions

Stop and ask for human review when:

- A business rule conflicts with current implementation.
- A service boundary is unclear.
- A payment or security decision affects money or vehicle exit.
- A migration would break existing data.
- Offline conflict handling cannot be resolved safely.

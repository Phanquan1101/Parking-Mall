# TASKS.md - ParkFlow Mall Task Breakdown

Priority: P0 = core MVP, P1 = Should Have/follow-on, P2 = optional hardening.
Status values: TODO, IN_PROGRESS, BLOCKED, DONE.

Audit/Fraud is cross-cutting. Add audit and suspicious-state acceptance criteria to each relevant slice; there is no separate Audit/Fraud slice.

## Slice 0 - Repository Foundation

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0001 | Create monorepo structure | P0 | Required folders exist | DONE |
| T-0002 | Add Docker Compose placeholder | P0 | Services can be added incrementally; no business services implemented | DONE |
| T-0003 | Add `.env.example` | P0 | DB, JWT, storage, payment mode, and sync variables documented | DONE |
| T-0004 | Document health-check convention | P0 | `/actuator/health` or equivalent is specified | DONE |
| T-0005 | Document shared API error format | P0 | Error JSON shape is specified | DONE |

## Slice 1 - API Gateway + Identity Skeleton

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0101 | Prepare identity-service skeleton | P0 | Structure and health contract documented | DONE |
| T-0102 | Draft users, roles, and JWT contract | P0 | Admin, Parking Staff, Merchant Staff roles defined | DONE |
| T-0103 | Draft login contract | P0 | `/api/auth/login` request/response documented | DONE |
| T-0104 | Draft protected-route authorization | P0 | Unauthorized and forbidden behavior documented | DONE |
| T-0105 | Prepare API Gateway route map | P0 | Canonical paths route to owning services | DONE |

## Slice 2 - Parking Session + QR Lookup

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0201 | Draft parking session schema | P0 | Canonical session states and fields documented | DONE |
| T-0202 | Draft check-in contract | P0 | Staff-confirmed plate creates an official session | DONE |
| T-0203 | Draft QR Lookup Token contract | P0 | Opaque lookup token exposes no sensitive data | DONE |
| T-0204 | Draft public ticket lookup | P0 | `/api/public/tickets/{lookupToken}` returns safe data only | DONE |
| T-0205 | Add parking audit acceptance criteria | P0 | Check-in and plate confirmation are auditable | DONE |

## Slice 3 - Customer Ticket Page

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0301 | Implement customer ticket page contract | P0 | Session summary, fee estimate, and payment state render from the public endpoint | DONE |
| T-0302 | Implement mobile-safe customer data | P0 | Only public ticket fields render; no staff, token metadata, or exit authorization is exposed | DONE |
| T-0303 | Implement ticket error states | P0 | Empty/invalid token and generic retrieval failures show safe actionable states | DONE |

## Slice 4 - Payment Simulation

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0401 | Implement payment order schema | P0 | In-memory order, transaction model, and idempotency state exist | DONE |
| T-0402 | Implement payment order endpoint | P0 | `/api/payments/orders` creates an order for a valid lookup token | DONE |
| T-0403 | Implement simulation success endpoint | P0 | `/api/payments/simulations/success` requires `Idempotency-Key` | DONE |
| T-0404 | Implement amount/code matching | P0 | Mismatch becomes `MISMATCHED` and cannot pay Parking | DONE |
| T-0405 | Record payment update event | P0 | Parking records payment confirmation event | DONE |

## Slice 5 - Dynamic Exit Pass + Check-out

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0501 | Implement Exit Pass schema | P0 | In-memory TTL, one-time status, session binding, and documentation exist | DONE |
| T-0502 | Implement Exit Pass creation/validation | P0 | Valid only for paid or zero-fee session; replacement invalidates active pass | DONE |
| T-0503 | Implement check-out contract | P0 | Plate match and valid unexpired/unused pass are required | DONE |
| T-0504 | Implement manual override contract | P0 | Authorized role, paid/zero-fee session, reason, and lightweight audit event required | DONE |
| T-0505 | Add suspicious/fraud acceptance criteria | P0 | Plate mismatch and pass replay are rejected and lightweight events are recorded | DONE |

## Slice 6 - Offline Staff Mode

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0601 | Document Online/Offline/Syncing state | P0 | Staff status is visible | TODO |
| T-0602 | Draft local active-session cache | P0 | Minimum data and TTL are specified | TODO |
| T-0603 | Draft offline event queue | P0 | Event ID, device ID, staff ID, timestamp, and `Idempotency-Key` are required | TODO |
| T-0604 | Draft offline sync API | P0 | `/api/parking/offline-sync` is idempotent | TODO |
| T-0605 | Document conflict handling | P0 | Accepted, rejected, duplicate, and conflict states are visible | TODO |
| T-0606 | Add sync audit/fraud criteria | P0 | Conflicts and manual offline exits are logged | TODO |

## Slice 7 - Merchant Invoice Aggregation

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0701 | Draft merchant validation schema | P0 | Tenant, staff profile, validation, and discount tables documented | TODO |
| T-0702 | Draft invoice validation endpoint | P0 | Authorized tenant and online-only official validation enforced | TODO |
| T-0703 | Enforce one invoice one use | P0 | Duplicate invoice code rejected and logged | TODO |
| T-0704 | Implement aggregate policy contract | P0 | Multiple invoices reach threshold under `AGGREGATE_INVOICE` | TODO |
| T-0705 | Document discount recalculation | P0 | Final fee never becomes negative | TODO |

## Slice 8 - Payment Reconciliation

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0801 | Draft reconciliation job schema | P0 | Jobs and items track pending/mismatched orders | TODO |
| T-0802 | Draft reconciliation endpoint | P0 | `/api/payments/reconciliation/run` is documented | TODO |
| T-0803 | Reconcile webhook/simulation retries | P0 | Duplicate events do not double-pay | TODO |
| T-0804 | Reconcile parking/reservation state | P0 | Inconsistent state becomes resolved or manual review | TODO |
| T-0805 | Add reconciliation dashboard/audit criteria | P0 | Pending and mismatched states remain visible and logged | TODO |

## Slice 9 - Reservation Basic

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0901 | Draft reservation and slot-hold schema | P1 | Hold expiry and canonical statuses documented | TODO |
| T-0902 | Draft reservation endpoint set | P1 | Create, retrieve, cancel, check-in, and reconciliation paths documented | TODO |
| T-0903 | Confirm reservation after payment | P1 | Confirmation requires paid order and valid hold | TODO |
| T-0904 | Release expired/unpaid holds | P1 | Expired/no-show behavior documented | TODO |
| T-0905 | Handle `PENDING_RECONCILIATION` | P1 | Payment success/confirmation failure is recoverable | TODO |

## Slice 10 - OCR Assist

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-1001 | Draft vision-service contract | P1 | Plate candidate and confidence are documented | TODO |
| T-1002 | Draft staff confirmation/correction flow | P0 | Manual plate entry remains authoritative | TODO |
| T-1003 | Document OCR timeout fallback | P0 | OCR failure never blocks check-in/check-out | TODO |
| T-1004 | Add plate correction audit criteria | P1 | Corrections are logged | TODO |

## Slice 11 - Dashboard

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-1101 | Draft dashboard summary contract | P0 | Active sessions and confirmed revenue are separated from pending data | TODO |
| T-1102 | Add payment state cards | P0 | Pending/mismatched/reconciliation states visible | TODO |
| T-1103 | Add merchant and offline cards | P0 | Validation totals, pending sync, and conflicts visible | TODO |
| T-1104 | Add suspicious/fraud view | P0 | Suspicious cases and alerts are visible to Admin | TODO |

## Slice 12 - Docker + Deploy Hardening

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-1201 | Complete Docker Compose | P0 | Local integration path documented | TODO |
| T-1202 | Document Railway/Supabase/hybrid deployment | P1 | Service mapping, ports, storage, and webhook ingress documented | TODO |
| T-1203 | Verify environment variable contract | P1 | Deployment variables match `.env.example` | TODO |
| T-1204 | Prepare demo readiness notes | P1 | Demo covers core flow, offline, QR security, aggregation, and reconciliation | TODO |

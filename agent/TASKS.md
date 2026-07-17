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
| T-0601 | Implement Online/Offline/Syncing state | P0 | Staff Offline Console visibly shows connection and sync state | DONE |
| T-0602 | Define basic local queue scope | P0 | localStorage queue stores only offline check-in events for Slice 6 | DONE |
| T-0603 | Implement offline event queue | P0 | Event ID, device ID, local timestamp, payload, and idempotency key persist locally | DONE |
| T-0604 | Implement offline sync API | P0 | `/api/parking/offline-sync` is protected and idempotent per event | DONE |
| T-0605 | Implement conflict handling | P0 | `SYNCED`, `DUPLICATE`, `REJECTED`, and `CONFLICT` remain visible | DONE |
| T-0606 | Add sync audit/fraud criteria | P0 | Conflicts/rejections emit lightweight events; offline check-out remains unsupported | DONE |

## Slice 7 - Merchant Invoice Aggregation

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0701 | Implement merchant validation model | P0 | In-memory tenant and invoice validation model exists | DONE |
| T-0702 | Implement invoice validation endpoint | P0 | Authorized online merchant/admin validation enforced | DONE |
| T-0703 | Enforce one invoice one use | P0 | Duplicate invoice code rejected | DONE |
| T-0704 | Implement aggregate policy | P0 | Multiple invoices aggregate under `AGGREGATE_INVOICE` | DONE |
| T-0705 | Implement discount recalculation | P0 | Final fee cannot become negative or exceed fee discount | DONE |

## Slice 8 - Payment Reconciliation

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0801 | Implement reconciliation records | P0 | In-memory items track pending/mismatched orders | DONE |
| T-0802 | Implement reconciliation endpoint | P0 | ADMIN run/list/get endpoints exist | DONE |
| T-0803 | Reconcile simulation retries | P0 | Failed Parking update can retry without double-pay | DONE |
| T-0804 | Reconcile Parking state | P0 | Update becomes resolved or remains visible for review | DONE |
| T-0805 | Add reconciliation visibility | P0 | API exposes pending/manual-review items | DONE |

## Slice 9 - Reservation Basic

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-0901 | Implement in-memory reservation model | P1 | Opaque code, expiry, and canonical Slice 9 statuses implemented | DONE |
| T-0902 | Implement basic reservation endpoints | P1 | Public create/get/cancel and staff list/expire endpoints exist | DONE |
| T-0903 | Consume reservation during Parking check-in | P1 | Internal token-protected consume creates a normal session only after validation | DONE |
| T-0904 | Release expired reservations | P1 | Stale `RESERVED` records become `EXPIRED` and release capacity | DONE |
| T-0905 | Defer reservation payment/reconciliation | P1 | Payment/deposit and `PENDING_RECONCILIATION` remain out of Slice 9 | DONE |

## Slice 10 - OCR Assist

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-1001 | Implement Vision OCR Assist endpoint | P1 | Authenticated image upload returns demo candidate and confidence | DONE |
| T-1002 | Implement staff confirmation/correction flow | P0 | OCR candidate can be edited before normal check-in | DONE |
| T-1003 | Preserve manual OCR fallback | P0 | OCR failure never blocks manual check-in | DONE |
| T-1004 | Add plate correction audit criteria | P1 | Corrections are logged | TODO |
| T-1005 | Add backend Gemini OCR provider | P1 | `DEMO_OCR` remains default; backend-only Gemini is configurable and mocked in tests | DONE |
| T-1006 | Implement Live Camera Gate Entry | P1 | Browser camera, throttled OCR assist, staff confirmation, and customer Lookup Ticket handoff work without Parking rule changes | DONE |
| T-1007 | Harden Gate Entry stability | P1 | Cooldown, single-submit guard, backoff, camera recovery, manual fallback, and safe reset work without changing Parking rules | DONE |
| T-1008 | Add local PaddleOCR provider | P1 | CPU-only `PADDLE_OCR` supports realtime camera assistance without an API key or external quota; staff confirmation/manual fallback remain mandatory | DONE |

## Slice 11 - Dashboard

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-1101 | Implement Slice 11A read-only dashboard | P0 | Client-side Parking and Reservation summaries render from existing APIs | DONE |
| T-1102 | Add payment state cards | P0 | Active/unpaid/paid-not-exited and ADMIN reconciliation visibility render | DONE |
| T-1103 | Add merchant and offline cards | P0 | Deferred: no listable source exists in Slice 11A | TODO |
| T-1104 | Add suspicious/fraud view | P0 | Deferred to a later dashboard slice | TODO |

## Slice 12 - Docker + Deploy Hardening

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| T-1201 | Complete Docker Compose | P0 | Local integration path documented | TODO |
| T-1202 | Document Railway/Supabase/hybrid deployment | P1 | Service mapping, ports, storage, and webhook ingress documented | TODO |
| T-1203 | Verify environment variable contract | P1 | Deployment variables match `.env.example` | TODO |
| T-1204 | Prepare demo readiness notes | P1 | Demo covers core flow, offline, QR security, aggregation, and reconciliation | TODO |

## UI-01 - Frontend Design System and App Shell Revamp

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| UI-0101 | Add responsive shared frontend shells | P1 | Staff/admin routes use `AppShell`; public/customer routes use `PublicShell`; route paths remain unchanged | DONE |
| UI-0102 | Establish lightweight design system | P1 | Shared tokens and consistent forms, cards, tables, buttons, badges, focus states, and responsive layout exist without a new UI framework | DONE |
| UI-0103 | Polish core operational and customer routes | P1 | Homepage and existing core pages retain their behavior with responsive visual improvements | DONE |

## UI-02 - Staff Operations Pages Deep Polish

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| UI-0201 | Polish Gate Entry console | P1 | Camera state, OCR assistance, manual fallback, confirmation, and QR handoff remain clear without changing scan behavior | DONE |
| UI-0202 | Polish operations dashboard | P1 | Metrics, table statuses, empty states, and partial ADMIN reconciliation access are visually clear | DONE |
| UI-0203 | Polish Offline Staff and OCR Upload | P1 | Queue summaries, sync state, OCR upload/result, and confirmed check-in forms retain existing behavior with clearer presentation | DONE |

## UI-03 - Customer, Reservation, Merchant Pages Deep Polish

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| UI-0301 | Polish customer home and ticket | P1 | Homepage and mobile ticket make QR Lookup, payment simulation, discount, and Exit Pass boundaries clear without behavior changes | DONE |
| UI-0302 | Polish reservation create and detail | P1 | Reservation form, success code, detail status, cancellation control, and staff handoff remain route/API compatible | DONE |
| UI-0303 | Polish merchant validation | P1 | Merchant JWT guidance, invoice form, aggregate policy, result, and error presentation retain existing validation behavior | DONE |

## UI-04 - Final Demo Flow Polish and Visual QA

| Task ID | Task | Priority | Acceptance Criteria | Status |
|---|---|---|---|---|
| UI-0401 | Align final demo flow | P1 | Homepage supports the approved Reservation → Gate → Ticket → Merchant → Payment → Exit Pass → Dashboard story without new behavior | DONE |
| UI-0402 | Final consistency and responsive QA | P1 | Shared labels, badges, alerts, copy controls, spacing, mobile ticket, and staff table fallbacks are consistent | DONE |
| UI-0403 | Final build and visual-QA documentation | P1 | Web build passes and manual smoke/error/responsive checks are recorded | DONE |

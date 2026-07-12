# TEST_MATRIX.md — ParkFlow Mall Test Matrix

## 1. Testing principles

Every vertical slice must include:

- Happy path
- Invalid input
- Unauthorized role
- Business rule violation
- Idempotency/retry case where relevant
- Offline/sync case where relevant
- Audit log verification for sensitive actions

## 2. Test levels

| Level | Purpose | Tools/Approach |
|---|---|---|
| Unit tests | Validate business logic | JUnit, Mockito, service-level tests |
| Integration tests | Validate DB/API behavior | Spring Boot tests, Testcontainers if possible |
| Contract tests | Validate API request/response | API client tests, Postman/Bruno |
| E2E smoke tests | Validate core workflow | Manual or Playwright later |
| Offline tests | Validate local queue/sync | Browser offline mode, simulated network cut |
| Payment tests | Validate simulation/webhook/idempotency | Simulated webhook payloads |

## 3. Slice 1 identity and gateway tests

| Test ID | Scenario | Expected Result | Priority |
|---|---|---|---|
| TC-AUTH-001 | Admin logs in with valid demo credentials | JWT response contains Bearer token, expiry, user, and `ADMIN` role | P0 |
| TC-AUTH-002 | Login uses wrong password | `401 Unauthorized` | P0 |
| TC-AUTH-003 | Request `/api/auth/me` without bearer token | `401 Unauthorized` | P0 |
| TC-AUTH-004 | Request `/api/auth/me` with valid token | Current user and role list returned | P0 |
| TC-GATE-001 | Gateway application context loads | Gateway configuration starts | P0 |
| TC-GATE-002 | Gateway login route | Request is proxied to Identity Service | P0 |
| TC-GATE-003 | Gateway profile route | Authorization header is forwarded to Identity Service | P0 |

## 4. Core parking tests

| Test ID | Scenario | Expected Result | Priority |
|---|---|---|---|
| TC-PARK-001 | Staff creates valid check-in session | Session ACTIVE, QR Lookup token generated | P0 |
| TC-PARK-002 | Duplicate active plate check-in | Request rejected with business rule error | P0 |
| TC-PARK-003 | Customer opens QR Lookup Ticket | Safe ticket info returned, plate masked | P0 |
| TC-PARK-004 | QR Lookup used as Exit Pass | Rejected | P0 |
| TC-PARK-005 | Staff checks out PAID session with valid Exit Pass and matching plate | Session EXITED | P0 |
| TC-PARK-006 | Staff checks out unpaid session | Rejected with PAYMENT_REQUIRED | P0 |
| TC-PARK-007 | Exit plate mismatch | Session SUSPICIOUS, gate denied | P0 |
| TC-PARK-008 | Manual override without reason | Rejected | P0 |
| TC-PARK-009 | Manual override with reason and correct role | Gate decision allowed, audit log created | P0 |
| TC-PARK-010 | Reuse Exit Pass after successful check-out | Rejected TOKEN_ALREADY_USED | P0 |
| TC-PARK-011 | Expired Exit Pass | Rejected TOKEN_EXPIRED | P0 |

### Slice 2 implemented coverage

| Test ID | Scenario | Expected Result | Status |
|---|---|---|---|
| TC-S2-PARK-001 | Authorized staff check-in | ACTIVE/UNPAID session, code, and opaque lookup token returned | Implemented |
| TC-S2-PARK-002 | Plate normalization | Spaces and dashes are removed; plate is uppercased | Implemented |
| TC-S2-PARK-003 | Missing plate | `400 Bad Request` | Implemented |
| TC-S2-PARK-004 | Duplicate ACTIVE normalized plate | `409 Conflict` | Implemented |
| TC-S2-PARK-005 | Public ticket lookup | Ticket lookup works without login | Implemented |
| TC-S2-PARK-006 | Invalid public token | Safe `404 Not Found` | Implemented |
| TC-S2-PARK-007 | Public ticket response | Does not expose staff ID or token metadata | Implemented |
| TC-S2-PARK-008 | Protected check-in without JWT | `401 Unauthorized` | Implemented |
| TC-S2-PARK-009 | Merchant role check-in attempt | `403 Forbidden` | Implemented |
| TC-S2-PARK-010 | Check-out route guard | No check-out endpoint exists in Slice 2 | Implemented |

### Slice 3 implemented coverage

| Test ID | Scenario | Expected Result | Status |
|---|---|---|---|
| TC-S3-WEB-001 | Customer opens a valid lookup URL | Public ticket summary renders without login | Implemented |
| TC-S3-WEB-002 | Customer opens an invalid lookup URL | Safe `Ticket not found or no longer valid.` state renders | Implemented |
| TC-S3-WEB-003 | Public ticket request is pending | `Loading ticket...` state is visible | Implemented |
| TC-S3-WEB-004 | Public ticket request fails outside `404` | Retryable generic error state is visible | Implemented |
| TC-S3-WEB-005 | Customer ticket summary renders | Vehicle, parking, fee, and status fields render from the public contract | Implemented |
| TC-S3-WEB-006 | Customer views ticket security notice | UI states that QR Lookup cannot authorize vehicle exit | Implemented |
| TC-S3-WEB-007 | Customer ticket page data exposure | No `staffId`, raw lookup token, audit data, or security internals are rendered | Implemented |

### Slice 4 implemented coverage

| Test ID | Scenario | Expected Result | Status |
|---|---|---|---|
| TC-S4-PAY-001 | Create payment order with valid ticket | Pending order uses the configured demo fee | Implemented |
| TC-S4-PAY-002 | Simulation without `Idempotency-Key` | Request is rejected | Implemented |
| TC-S4-PAY-003 | Simulation retry with same key | Existing order result is returned without double-processing | Implemented |
| TC-S4-PAY-004 | Code or amount mismatch | Order becomes `MISMATCHED`; Parking is not paid | Implemented |
| TC-S4-PAY-005 | Internal payment update | Valid internal token marks session `PAID`; invalid token is rejected | Implemented |
| TC-S4-PAY-006 | Customer ticket after simulation | Public ticket shows `PAID`; no Exit Pass is displayed | Implemented |

### Slice 5 implemented coverage

| Test ID | Scenario | Expected Result | Status |
|---|---|---|---|
| TC-S5-EXIT-001 | Paid session creates Exit Pass | Opaque `ACTIVE` pass has expiry and 60-second default TTL | Implemented |
| TC-S5-EXIT-002 | Unpaid session creates Exit Pass | Rejected with `SESSION_NOT_PAID` | Implemented |
| TC-S5-EXIT-003 | Invalid or other-session lookup token | Exit Pass creation is rejected | Implemented |
| TC-S5-EXIT-004 | Replacement Exit Pass | Previous active pass becomes `INVALIDATED` | Implemented |
| TC-S5-EXIT-005 | Valid pass and matching plate | Staff validation returns `valid=true` | Implemented |
| TC-S5-EXIT-006 | Expired, unknown, or used pass | Safe rejection with specific Exit Pass error code | Implemented |
| TC-S5-EXIT-007 | QR Lookup supplied as Exit Pass | Rejected as `EXIT_PASS_NOT_FOUND` | Implemented |
| TC-S5-EXIT-008 | Plate mismatch | Validation/check-out rejected and lightweight event recorded | Implemented |
| TC-S5-EXIT-009 | Valid check-out | Session becomes `EXITED` and pass becomes `USED` | Implemented |
| TC-S5-EXIT-010 | Manual override | Requires paid/zero-fee session and nonblank reason; records event | Implemented |
| TC-S5-SEC-001 | No JWT or merchant JWT at staff validation | `401` or `403`; ADMIN and PARKING_STAFF accepted | Implemented |
| TC-S5-GATE-001 | Gateway Exit Pass routes | Public create and protected validate/check-out routes forward correctly | Implemented |
| TC-S5-WEB-001 | Paid customer ticket | Generate Exit Pass button and issued-pass warning render | Build verified |

### Slice 6 implemented coverage

| Test ID | Scenario | Expected Result | Status |
|---|---|---|---|
| TC-S6-OFF-001 | Sync without JWT or with merchant JWT | `401` / `403` | Implemented |
| TC-S6-OFF-002 | Sync without `Idempotency-Key` | `400 Bad Request` | Implemented |
| TC-S6-OFF-003 | Valid OFFLINE_CHECK_IN | Official `ACTIVE` server session, QR Lookup Token, ID, and code | Implemented |
| TC-S6-OFF-004 | Missing plate or invalid vehicle type | Per-event `REJECTED` result | Implemented |
| TC-S6-OFF-005 | Duplicate event ID or event idempotency key | `DUPLICATE`; no second session | Implemented |
| TC-S6-OFF-006 | Existing active normalized plate | Per-event `CONFLICT`; server state wins | Implemented |
| TC-S6-OFF-007 | Event result lookup | Stored result returned; unknown event is `404` | Implemented |
| TC-S6-GATE-001 | Gateway sync and status routes | Authorization and idempotency header are forwarded | Implemented |
| TC-S6-WEB-001 | Offline Staff Console | Local queue, connection state, and build verified | Implemented |

## 5. Payment tests

| Test ID | Scenario | Expected Result | Priority |
|---|---|---|---|
| TC-PAY-001 | Create payment order for unpaid session | Payment order PENDING | P0 |
| TC-PAY-002 | Simulate payment success with correct amount/code | Order PAID, session payment PAID, grace period set | P0 |
| TC-PAY-003 | Duplicate simulation webhook | No duplicate payment transaction | P0 |
| TC-PAY-004 | Simulation amount mismatch | Payment manual review or rejected | P0 |
| TC-PAY-005 | Simulation wrong payment code | Unmatched/manual review | P0 |
| TC-PAY-006 | Payment reconciliation resolves pending order | Status updated or manual review | P1 |
| TC-PAY-007 | SePay duplicate provider transaction | No double-pay | P1 |
| TC-PAY-008 | Webhook saved before processing | Raw event exists even if processing fails | P1 |

## 6. QR security tests

| Test ID | Scenario | Expected Result | Priority |
|---|---|---|---|
| TC-QR-001 | QR Lookup token reveals only safe ticket info | No raw sensitive data beyond allowed display | P0 |
| TC-QR-002 | Screenshot of QR Lookup after payment but no Exit Pass | Cannot auto-exit | P0 |
| TC-QR-003 | Generate Exit Pass before payment | Rejected | P0 |
| TC-QR-004 | Generate Exit Pass after payment | Short-lived token returned | P0 |
| TC-QR-005 | Use Exit Pass twice | Second use rejected | P0 |
| TC-QR-006 | Use Exit Pass after TTL | Rejected | P0 |

## 7. Offline tests

| Test ID | Scenario | Expected Result | Priority |
|---|---|---|---|
| TC-OFF-001 | Staff Console goes offline | UI shows Offline status | P0 |
| TC-OFF-002 | Offline check-in event created | Event stored locally with event_id | P0 |
| TC-OFF-003 | Sync offline check-in when online | Server accepts event and creates/links session | P0 |
| TC-OFF-004 | Duplicate offline sync | Server returns DUPLICATE | P0 |
| TC-OFF-005 | Offline check-out with local cache and manual reason | Event queued and later synced | P0 |
| TC-OFF-006 | Offline event conflicts with server session EXITED | Sync result CONFLICT | P0 |
| TC-OFF-007 | Dashboard shows pending sync count | Admin sees pending/conflict status | P0 |
| TC-OFF-008 | Offline queue survives browser refresh | Events remain until sync | P1 |

## 8. Merchant validation tests

| Test ID | Scenario | Expected Result | Priority |
|---|---|---|---|
| TC-MER-001 | Merchant adds invoice below threshold | Invoice accepted, no discount applied yet | P0 |
| TC-MER-002 | Merchant adds second invoice and aggregate reaches threshold | Discount applied | P0 |
| TC-MER-003 | Duplicate invoice code | Rejected and fraud alert created | P0 |
| TC-MER-004 | Merchant validates session already EXITED | Rejected | P0 |
| TC-MER-005 | Merchant validates invoice for tenant they do not belong to | Forbidden | P0 |
| TC-MER-006 | Discount exceeds fee | Final fee becomes 0, not negative | P0 |

## 9. Reservation tests

| Test ID | Scenario | Expected Result | Priority |
|---|---|---|---|
| TC-RES-001 | Customer creates reservation | PENDING_PAYMENT reservation and slot hold created | P1 |
| TC-RES-002 | Payment success before hold expiry | Reservation CONFIRMED | P1 |
| TC-RES-003 | Payment not completed before hold expiry | Reservation EXPIRED, hold released | P1 |
| TC-RES-004 | Payment success arrives after reservation expired | Manual review/reconciliation state | P1 |
| TC-RES-005 | Check-in within check-in window | Parking session created | P1 |
| TC-RES-006 | Check-in too early | Rejected with business message | P1 |
| TC-RES-007 | Check-in too late | EXPIRED/NO_SHOW or normal parking fallback | P1 |

## 10. OCR tests

| Test ID | Scenario | Expected Result | Priority |
|---|---|---|---|
| TC-OCR-001 | Upload clear plate image | Plate and confidence returned | P1 |
| TC-OCR-002 | OCR low confidence | Staff must confirm/edit manually | P1 |
| TC-OCR-003 | OCR timeout | Staff can continue with manual plate entry | P1 |
| TC-OCR-004 | OCR service unavailable | Parking flow remains usable manually | P1 |

## 11. Dashboard tests

| Test ID | Scenario | Expected Result | Priority |
|---|---|---|---|
| TC-DASH-001 | Dashboard summary loads | Active sessions, revenue, suspicious, pending shown | P0 |
| TC-DASH-002 | Payment changes to PAID | Revenue/payment counts update | P0 |
| TC-DASH-003 | Offline event pending | Pending sync count visible | P0 |
| TC-DASH-004 | Suspicious session created | Alert count visible | P0 |
| TC-DASH-005 | Merchant discount applied | Discount total visible | P1 |

## 12. Security/access tests

| Test ID | Scenario | Expected Result | Priority |
|---|---|---|---|
| TC-SEC-001 | Unauthenticated user accesses staff endpoint | 401 | P0 |
| TC-SEC-002 | Merchant accesses parking check-out endpoint | 403 | P0 |
| TC-SEC-003 | Parking staff accesses admin dashboard | 403 unless allowed | P0 |
| TC-SEC-004 | Public ticket token guesses random token | 404 or invalid token | P0 |
| TC-SEC-005 | Image URL direct access without permission | Denied or signed URL required | P1 |

## 13. Documentation alignment additions

The following cases close the remaining BRD/PRD rule coverage gaps and are mapped to canonical rule IDs.

| Test ID | Rule ID | Scenario | Expected Result | Priority |
|---|---|---|---|---|
| TC-PARK-012 | BR-PARK-001 | Same normalized plate attempts a second active session in the same lot | Request rejected | P0 |
| TC-PARK-013 | BR-PARK-005 | Payment completes and grace period is applied | Grace deadline is stored and honored | P0 |
| TC-PAY-009 | BR-PAY-002 | Attempt to select SePay Live in MVP | Mode rejected/disabled | P0 |
| TC-PAY-010 | BR-PAY-007 | Pending or mismatched payment enters reconciliation | Reconciliation resolves or sends to manual review | P0 |
| TC-OFF-009 | BR-OFF-003 | Offline exit lacks evidence and reason | Exit is not auto-authorized; manual review required | P0 |
| TC-OFF-010 | BR-OFF-005 | Offline event conflicts with newer server state | Server state wins and conflict remains visible | P0 |
| TC-MER-007 | BR-MER-004 | Merchant staff submits invoice for unauthorized tenant | Request rejected | P0 |
| TC-MER-008 | BR-MER-005 | Official merchant validation while offline | Official validation rejected or held as draft | P0 |
| TC-MER-009 | BR-MER-006 | Discount exceeds parking fee | Final fee is zero, never negative | P0 |
| TC-RES-008 | BR-RES-002 | Reservation slot hold reaches expiry | Hold released and reservation expires | P1 |
| TC-RES-009 | BR-RES-003 | Payment succeeds but reservation confirmation fails | Reservation becomes `PENDING_RECONCILIATION` | P1 |
| TC-RES-010 | BR-RES-005 | Refund flow requested in MVP | Feature is unavailable/out of scope | P1 |
| TC-OCR-005 | BR-OCR-001 | OCR candidate conflicts with manually entered plate | Staff value remains authoritative | P1 |
| TC-AUD-001 | BR-AUD-005 | Attempt to edit/delete audit log through MVP UI | Operation is unavailable/denied | P0 |
| TC-AUD-002 | BR-AUD-004 | Payment amount or code mismatch | Mismatch is logged and review state is visible | P0 |

Every test should retain the baseline happy-path, invalid-input, unauthorized-role, idempotency, offline, and audit checks required by the project rules.

## 14. Canonical scenario checklist

| Scenario | Expected Result | Rule/Area |
|---|---|---|
| QR Lookup cannot authorize exit | Gate decision is denied without a valid Exit Pass | BR-QR-002 |
| Exit Pass expired is rejected | `TOKEN_EXPIRED` | BR-QR-005 |
| Exit Pass reused is rejected | `TOKEN_ALREADY_USED` | BR-QR-005 |
| Payment simulation success | Order and eligible session become `PAID` | BR-PAY-001 |
| Duplicate simulation event idempotency | No duplicate payment or double-pay | BR-PAY-003 |
| Offline conflict | Sync returns visible `CONFLICT` | BR-OFF-006 |
| OCR failure falls back to manual entry | Staff can continue without OCR | BR-OCR-004 |
| Manual override creates audit log | Override requires reason and creates audit record | BR-AUD-001 |

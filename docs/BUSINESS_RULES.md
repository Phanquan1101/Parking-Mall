# Business Rules

Canonical source: BRD v2, PRD v2, and SAD v1 after the accepted alignment decisions in `docs/DECISION_LOG.md`.

## A. Parking Session Rules

- BR-PARK-001: A parking lot must not have more than one active session for the same normalized plate.
- BR-PARK-002: Staff must confirm the plate before an official parking session is created.
- BR-PARK-003: A plate mismatch at exit creates a `SUSPICIOUS` state and requires review or an authorized manual override.
- BR-PARK-004: Automatic check-out requires session payment status `PAID` or `final_fee = 0`.
- BR-PARK-005: The configured grace period applies after payment before additional fee handling.

## B. QR Security Rules

- BR-QR-001: QR Lookup Token is for ticket/session lookup only.
- BR-QR-002: QR Lookup Token cannot authorize vehicle exit.
- BR-QR-003: Normal check-out after payment requires a valid Dynamic Exit Pass.
- BR-QR-004: Dynamic Exit Pass has a configured TTL and is one-time use.
- BR-QR-005: Expired, used, revoked, or invalid Exit Passes must be rejected.
- BR-QR-006: A screenshot of QR Lookup is not sufficient for automatic exit.

## C. Payment Rules

- BR-PAY-001: Payment Simulation Mode must be implemented before SePay Test Mode.
- BR-PAY-002: SePay Live Mode is disabled and out of MVP.
- BR-PAY-003: Payment webhook and simulation processing must be idempotent.
- BR-PAY-004: Idempotency for payment mutations must use the HTTP `Idempotency-Key` header.
- BR-PAY-005: Payment is valid only when `payment_code` and amount match the order.
- BR-PAY-006: Payment state must support `MISMATCHED` and `PENDING_RECONCILIATION`.
- BR-PAY-007: A reconciliation job must process pending and mismatched payment states.

## D. Offline Rules

- BR-OFF-001: Staff Console must show `Online`, `Offline`, or `Syncing` state.
- BR-OFF-002: Offline check-in may create a local offline event and temporary session reference.
- BR-OFF-003: Offline check-out is limited and requires local evidence or an authorized manual reason.
- BR-OFF-004: Offline events must include event ID, device ID, staff ID, timestamp, and `Idempotency-Key`.
- BR-OFF-005: Server state is the final source of truth after synchronization.
- BR-OFF-006: Synchronization conflicts must remain visible for staff/admin review.

## E. Merchant Invoice Rules

- BR-MER-001: `AGGREGATE_INVOICE` is the default policy.
- BR-MER-002: Multiple valid invoices may be added to one parking session.
- BR-MER-003: One invoice code can be used only once across the system.
- BR-MER-004: Merchant staff may validate only invoices for an authorized tenant.
- BR-MER-005: Official merchant validation requires online mode.
- BR-MER-006: Discount recalculation must not make the final parking fee negative.

## F. Reservation Rules

- BR-RES-001: Reservation Basic is Should Have and must not block the core MVP flow.
- BR-RES-002: A slot hold must expire after its configured payment window.
- BR-RES-003: Reservation may enter `PENDING_RECONCILIATION` when payment succeeds but confirmation fails.
- BR-RES-004: Payment success and reservation confirmation failures require reconciliation.
- BR-RES-005: Refund processing is out of MVP.

## G. OCR Rules

- BR-OCR-001: OCR is assistive only and is not authoritative.
- BR-OCR-002: Manual plate entry is Must Have.
- BR-OCR-003: Staff final plate confirmation/correction is required.
- BR-OCR-004: OCR failure or timeout must not block check-in or check-out.

## H. Audit/Fraud Rules

- BR-AUD-001: Manual override requires an authorized role, reason, and audit log.
- BR-AUD-002: Plate correction must be audited.
- BR-AUD-003: Duplicate invoice attempts must be logged and may create a fraud alert.
- BR-AUD-004: Payment mismatches must be logged.
- BR-AUD-005: Audit logs must not be mutable through the MVP UI.
- BR-AUD-006: Dashboard must show suspicious, conflict, and pending states.

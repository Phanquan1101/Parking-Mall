# DECISION_LOG.md - ParkFlow Mall Decisions and Assumptions

All decisions below are accepted and form the documentation source of truth for the MVP.

## ADR-001

- Status: Accepted
- Decision: Use a microservice-oriented architecture for ParkFlow Mall.
- Rationale: The product has distinct identity, parking, payment, merchant, reservation, reporting, and vision responsibilities.
- Consequences: Service boundaries are documented even where the MVP shares one database project.

## ADR-002

- Status: Accepted
- Decision: Use schema-per-service on one Supabase PostgreSQL project for MVP.
- Rationale: This preserves service ownership while controlling MVP cost and operational complexity.
- Consequences: Services must not directly modify another service's schema.

## ADR-003

- Status: Accepted
- Decision: Payment Simulation Mode must be implemented before SePay Test Mode.
- Rationale: Simulation reduces integration and reconciliation risk while proving the payment lifecycle.
- Consequences: Simulation is the default MVP mode; SePay Test is later readiness work.

## ADR-004

- Status: Accepted
- Decision: SePay Live Mode is out of MVP and must remain disabled.
- Rationale: Live money flow and production reconciliation are outside the approved MVP risk boundary.
- Consequences: No live payment endpoint or live credentials may be enabled for MVP.

## ADR-005

- Status: Accepted
- Decision: QR Lookup Token cannot authorize vehicle exit.
- Rationale: Lookup QR screenshots must not become gate credentials.
- Consequences: Exit authorization requires a separate Exit Pass and validation.

## ADR-006

- Status: Accepted
- Decision: Dynamic Exit Pass is required for normal check-out after payment.
- Rationale: Payment state and exit authorization must remain separate security decisions.
- Consequences: Paid or zero-fee sessions must obtain a valid Exit Pass before normal exit.

## ADR-007

- Status: Accepted
- Decision: Dynamic Exit Pass must be short-lived and one-time use.
- Rationale: TTL and replay protection reduce screenshot and token-reuse risk.
- Consequences: Expired or used passes are rejected; the MVP default TTL is 60 seconds.

## ADR-008

- Status: Accepted
- Decision: Idempotency must use the HTTP `Idempotency-Key` header for payment and offline sync operations.
- Rationale: A consistent transport-level contract is required for safe retries.
- Consequences: Idempotency keys must not be supplied only in request bodies.

## ADR-009

- Status: Accepted
- Decision: Merchant validation default policy is `AGGREGATE_INVOICE`.
- Rationale: A customer may have valid purchases from multiple mall tenants.
- Consequences: Multiple invoices can contribute to one session, while each invoice code remains single-use.

## ADR-010

- Status: Accepted
- Decision: OCR Assist is Should Have; manual plate entry and staff confirmation are Must Have.
- Rationale: OCR quality varies with lighting, angle, and plate condition.
- Consequences: OCR is never authoritative and cannot block parking operations.

## ADR-011

- Status: Accepted
- Decision: Reservation Basic is Should Have and must not block core MVP.
- Rationale: Reservation adds distributed payment/slot complexity after the core parking flow.
- Consequences: Reservation follows Payment Reconciliation and supports `PENDING_RECONCILIATION`; refund/live payment remain out of MVP.

## ADR-012

- Status: Accepted
- Decision: Server state is the source of truth after offline sync.
- Rationale: Offline events may conflict with newer authoritative state.
- Consequences: Sync results may be accepted, rejected, or conflicted and must remain visible.

## ADR-013

- Status: Accepted
- Decision: Railway + Supabase + Docker Compose hybrid is the preferred MVP deployment strategy.
- Rationale: It provides low-cost local development, managed database/storage, and a reachable webhook endpoint.
- Consequences: Ports, URLs, and secrets must be environment-configured; no Railway proxy values may be hardcoded.

## ADR-014

- Status: Accepted
- Decision: Audit/Fraud is cross-cutting, not a separate implementation slice.
- Rationale: Sensitive actions across payment, parking, merchant, offline, and reservation flows must be auditable where they occur.
- Consequences: Each relevant slice includes audit/fraud acceptance criteria.

## ADR-015

- Status: Accepted
- Decision: Use independent Maven-based Spring Boot skeletons on Java 21 for Slice 0.
- Rationale: Maven 3.9 and Java 21 are available in the local development environment, and independent service builds keep the foundation simple.
- Consequences: Each Java service owns a minimal `pom.xml`, application entrypoint, Actuator health configuration, and context-load test. No parent reactor build is introduced yet.

## ADR-016

- Status: Accepted
- Decision: Use React + Vite + TypeScript for the web skeleton and FastAPI for the Vision Service skeleton.
- Rationale: These match the approved SAD stack while keeping Slice 0 lightweight.
- Consequences: The web app contains static placeholders only, and Vision Service exposes only `GET /health` until Slice 10.

## Remaining non-blocking questions

- Should OCR accept upload-only or camera frames first for the demo?
- Should dashboard export be included in a future hardening phase?
- Should optional reporting read models be materialized for the demo?

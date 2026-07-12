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

## ADR-017

- Status: Accepted
- Decision: Slice 1 uses BCrypt-hashed in-memory demo users and signed JWT access tokens.
- Rationale: This creates a testable login and role foundation without adding a database or migration before the identity data model is approved.
- Consequences: `admin`, `staff`, and `merchant` are local-demo accounts only and must be replaced by persistent user storage before production.

## ADR-018

- Status: Accepted
- Decision: API Gateway forwards bearer tokens to Identity Service; Identity Service validates JWTs.
- Rationale: Keeping token validation with the issuing service is the smallest maintainable Slice 1 boundary.
- Consequences: Gateway proxies only auth endpoints in this slice and does not perform parking or payment routing.

## ADR-019

- Status: Accepted
- Decision: Slice 2 stores parking sessions in an in-memory repository behind `ParkingSessionRepository`.
- Rationale: It enables the first check-in and lookup workflow without introducing a database, Supabase connection, or migration before persistence is approved.
- Consequences: Sessions disappear on restart and the repository implementation must be replaced by parking-schema persistence in a later slice.

## ADR-020

- Status: Accepted
- Decision: Parking Service independently validates Identity-issued JWTs using the shared `JWT_SECRET` configuration.
- Rationale: This avoids an Identity Service round-trip for each protected parking request while a shared auth package does not yet exist.
- Consequences: Identity and Parking Service must use compatible signing configuration; Gateway forwards the bearer token without validating it.

## ADR-021

- Status: Accepted
- Decision: Slice 3 uses `react-router-dom` for public ticket routing and `VITE_API_BASE_URL` for the frontend gateway base URL.
- Rationale: A parameterized browser route keeps QR ticket links shareable while the environment variable supports local gateway configuration without embedding deployment URLs in frontend code.
- Consequences: `/tickets/:lookupToken` is public, fetches only the public ticket endpoint, and must never surface lookup-token metadata or exit authorization controls.

## ADR-022

- Status: Accepted
- Decision: Slice 4 stores payment orders and idempotency results in memory behind repository interfaces.
- Rationale: This proves the simulation lifecycle before a payment database schema or migration is approved.
- Consequences: Payment data is reset on service restart and must be replaced with persistent storage later.

## ADR-023

- Status: Accepted
- Decision: Payment Service calls Parking's internal PAID-update endpoint using `X-Internal-Service-Token`.
- Rationale: The cross-service update must not be exposed through the public gateway.
- Consequences: Payment and Parking require the same local internal-token configuration.

## ADR-024

- Status: Accepted
- Decision: Use `PARKING_DEMO_FLAT_FEE=5000` until a fee engine is implemented.
- Rationale: Payment simulation needs a non-zero amount without introducing dynamic pricing or discounts.
- Consequences: The fee is a configurable demo-only value, not a parking pricing rule.

## ADR-025

- Title: Separate Hybrid Local and Full Docker host ports
- Status: Accepted
- Decision: Direct local backend uses `8080`–`8090`; Full Docker publishes `18080`–`18090`; infrastructure uses the `infra` profile; the full backend uses the `full-stack` profile. Internal container ports remain unchanged.
- Rationale: Prevent host-port conflicts, support IntelliJ debugging, and preserve full container testing.
- Consequences: Frontend API base URL differs by mode, documentation must clearly state the active mode, and Docker service-to-service URLs use internal Compose names.

## ADR-026

- Status: Accepted
- Decision: Slice 5 stores Dynamic Exit Passes in `InMemoryExitPassRepository` with a 60-second default TTL from `EXIT_PASS_TTL_SECONDS`.
- Rationale: It validates the secured exit workflow without adding a database migration before persistence is approved.
- Consequences: Exit Passes are reset on Parking Service restart and must later move to `parking_schema.exit_passes` with protected token storage.

## ADR-027

- Status: Accepted
- Decision: Creating a new Dynamic Exit Pass invalidates the previous active pass for the same session.
- Rationale: A single currently valid customer credential is the simpler and safer MVP behavior.
- Consequences: Staff reject invalidated passes; customers can regenerate a pass if the prior one expires.

## ADR-028

- Status: Accepted
- Decision: Manual override in Slice 5 bypasses only Exit Pass possession, never payment; it requires an authorized staff/admin role, a nonblank reason, and a lightweight event record.
- Rationale: Staff need a controlled lost-phone exception without creating an unpaid vehicle-exit path.
- Consequences: Plate mismatches are retained as a suspicious reason when override is used; full audit/fraud storage remains later work.

## ADR-029

- Status: Accepted
- Decision: Slice 6 stores the browser offline queue in localStorage and uses a stable locally generated device ID.
- Rationale: It is sufficient for a small demo queue while keeping the implementation ready to move to IndexedDB later.
- Consequences: The queue is browser/device scoped and contains only minimum check-in data; rejected/conflicted items remain visible.

## ADR-030

- Status: Accepted
- Decision: Slice 6 stores synced OfflineEvent results in `InMemoryOfflineEventRepository` and supports only `OFFLINE_CHECK_IN` as an official offline operation.
- Rationale: It proves event-level idempotency and conflict behavior without migrations or unsafe offline exit behavior.
- Consequences: Events are reset on Parking Service restart; offline check-out, payment, merchant validation, and reservation are not implemented.

## ADR-031

- Status: Accepted
- Decision: The server is source of truth after offline sync; duplicate event ID/idempotency keys return `DUPLICATE`, and active-plate conflicts return `CONFLICT`.
- Rationale: A local queue cannot safely decide authoritative parking state after reconnection.
- Consequences: The Staff Console preserves terminal conflict/rejection results for manual follow-up.

## ADR-032

- Status: Accepted
- Decision: Slice 7 keeps invoice validations in memory and maps demo username `merchant` to `tenant-demo-001`; `ADMIN` has demo override access.
- Rationale: This proves tenant-scoped online validation without a migration or POS integration.

## ADR-033

- Status: Accepted
- Decision: Use temporary `AGGREGATE_INVOICE` threshold `300000` and discount `5000`, capped at estimated parking fee.
- Rationale: It demonstrates aggregation while deferring real merchant rules and accounting.

## Remaining non-blocking questions

- Should OCR accept upload-only or camera frames first for the demo?
- Should dashboard export be included in a future hardening phase?
- Should optional reporting read models be materialized for the demo?

# CODING_RULES.md — ParkFlow Mall Coding Rules

## 1. General rules

- Keep code simple and testable.
- Implement one vertical slice at a time.
- Do not create abstractions until needed by at least two real use cases.
- Do not silently change business rules.
- Do not add out-of-scope features.
- Do not put secrets in source code.
- Use environment variables for all service URLs, DB credentials, JWT secrets, provider settings.

## 2. Java Spring Boot rules

Recommended stack:

- Java 17 or 21
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Bean Validation
- Flyway or Liquibase
- Springdoc OpenAPI if practical
- Lombok optional
- MapStruct optional

Layering convention:

```text
controller -> application/service -> domain/business rules -> repository -> database
```

Rules:

- Controllers must be thin.
- Business rules must live in service/domain layer.
- Repository must not contain business decisions.
- Validate DTOs with Bean Validation and service-level checks.
- Use enums for statuses where possible.
- Use explicit error codes for business rule failures.
- Add audit log through a dedicated service.
- Use idempotency keys for check-in, check-out, payment simulation, webhook, and offline sync.

## 3. React frontend rules

Recommended stack:

- React + Vite
- TypeScript
- TailwindCSS
- React Router
- TanStack Query or Axios
- QR scanner library only where needed
- IndexedDB/local storage wrapper for offline queue

Rules:

- Customer pages must be mobile-first.
- Staff Console must clearly show Online / Offline / Syncing state.
- Merchant validation must be no more than three main steps.
- Error messages must tell the user what to do next.
- Do not store raw secrets in browser storage.
- Local offline cache must use TTL and only store minimum required data.

## 4. Python Vision Service rules

Recommended stack:

- Python
- FastAPI
- OpenCV
- YOLOv8/Ultralytics or chosen repo
- EasyOCR/PaddleOCR if needed

Rules:

- OCR must be assistive only.
- OCR failure must not block parking workflow.
- Response must include `plate`, `confidence`, and optional `bbox`.
- Add timeout handling on caller side.

## 5. API rules

- Use stable paths under `/api/{domain}`.
- Use JSON request/response.
- Public customer ticket endpoints must expose safe data only.
- Role-protected endpoints must be enforced by backend.
- Include request IDs or idempotency keys for mutation endpoints.
- Do not expose raw internal stack traces.
- Use consistent error format.

## 6. Database rules

- Use migrations for schema changes.
- Do not use `ddl-auto=update` for stable project stages.
- Use indexes for lookup tokens, payment codes, invoice codes, session status.
- Do not store raw QR/Exit Pass tokens if hashing is feasible.
- Store provider raw webhook payload for audit/debug.
- One invoice code must be unique globally in MVP.

## 7. Payment rules

- Default to Simulation Mode.
- Do not enable Live Mode without explicit approval.
- Store webhook raw payload before processing.
- Implement idempotency before marking order paid.
- Amount and payment code must match.
- Unmatched payments go to manual review.
- Payment state changes must create audit log.

## 8. Offline rules

- Offline mode is not optional.
- Offline events must have stable `eventId`.
- Offline queue must survive page refresh.
- Sync endpoint must be idempotent.
- Server is final source of truth.
- Conflicts must not be silently discarded.
- Dashboard must show pending sync/conflict counts.

## 9. QR security rules

- QR Lookup Token only opens ticket lookup.
- Dynamic Exit Pass is short-lived and one-time use.
- Screenshot of lookup QR cannot authorize exit.
- Plate verification is required for check-out.
- Manual override requires role and reason.

## 10. Documentation update rules

When behavior changes, update relevant docs:

- `docs/API_CONTRACT.md`
- `docs/DATABASE_SCHEMA.md`
- `docs/TEST_MATRIX.md`
- `docs/DECISION_LOG.md`

No feature is complete if docs are stale.

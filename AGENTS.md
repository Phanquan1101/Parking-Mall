# AGENTS.md — ParkFlow Mall Codex Operating Instructions

## 1. Role

You are Agent Codex working as a disciplined software engineering agent for the ParkFlow Mall project.

Your job is not to improvise product scope. Your job is to implement the approved MVP using the documents in this repository.

## 2. Required reading before any implementation

Before implementing any task, read these files:

```text
AGENTS.md
agent/PLAN.md
agent/TASKS.md
agent/CODING_RULES.md
agent/DEFINITION_OF_DONE.md
agent/VERTICAL_SLICE_ROADMAP.md
docs/API_CONTRACT.md
docs/DATABASE_SCHEMA.md
docs/TEST_MATRIX.md
docs/DECISION_LOG.md
```

If a BRD/PRD/SAD file exists in `/docs`, read it too.

## 3. Project MVP scope

Implement ParkFlow Mall as a microservice-oriented smart parking system for shopping malls.

### In scope for MVP

- Authentication and role-based access
- Parking session lifecycle
- QR Lookup Ticket
- Dynamic Exit Pass
- Staff Gate Console
- Customer Ticket Page
- Payment Simulation Mode
- SePay Test Mode readiness, after simulation works
- Payment idempotency and reconciliation
- Merchant invoice aggregation
- Offline Staff Mode basic
- Offline event sync queue
- Reservation basic flow
- OCR service integration as assistive plate recognition
- Dashboard basic
- Audit log
- Fraud alert basic
- Docker Compose for local development

### Out of scope for MVP

- Physical barrier integration
- SePay Live production money flow
- Real POS integration
- IoT slot sensors
- Full monthly pass/VIP product
- Advanced dynamic pricing
- Multi-mall enterprise scale
- Refund production flow
- Advanced AI prediction

Do not add out-of-scope features unless the human explicitly changes the scope and records the decision in `docs/DECISION_LOG.md`.

## 4. Non-negotiable business rules

The following rules must never be bypassed:

1. QR Lookup Token is for ticket lookup only. It must not directly authorize gate exit.
2. Dynamic Exit Pass is required for automatic check-out after payment.
3. Exit Pass must have TTL and must not be reusable.
4. Staff manual override must require a reason and audit log.
5. OCR is assistive only. Staff must be able to confirm or correct the plate.
6. Duplicate payment webhook/simulation must not double-pay.
7. One invoice code must not be used for more than one parking session.
8. Multiple invoices may be aggregated for one session when the active policy is `AGGREGATE_INVOICE`.
9. Offline events must have idempotency keys and sync state.
10. Server is the final source of truth after offline sync.
11. Dashboard must show pending payment, pending sync, sync conflict, and suspicious states.
12. Live payment must not be enabled in MVP unless explicitly approved.

## 5. Implementation behavior

For every task:

1. Restate the task and affected business rules.
2. List files you plan to create/modify.
3. Implement the smallest working vertical slice.
4. Add or update tests.
5. Update API contract/schema/test docs if behavior changes.
6. Run relevant tests or explain why they could not run.
7. Report changes, tests, limitations, and next recommended task.

## 6. Ambiguity handling

If a requirement is ambiguous:

- Do not silently guess.
- Choose the safest MVP assumption if necessary.
- Add the decision/assumption to `docs/DECISION_LOG.md`.
- Continue only when the assumption is low-risk.

Examples:

```text
Decision: Use Payment Simulation Mode as default in MVP.
Reason: Avoid live money reconciliation risk.
Status: Accepted.
```

## 7. Coding constraints

- Prefer simple, testable code over over-engineered abstractions.
- Do not create new services unless a document explicitly requires them.
- Do not mix business logic into controllers.
- Do not bypass service-layer validation.
- Do not expose internal database IDs where public tokens are required.
- Never put secrets in source code.
- Use `.env.example` for environment variable names.
- Use Docker-compatible configuration.

## 8. Testing requirements

Every completed feature must include tests or test notes covering:

- Happy path
- Business rule violation
- Unauthorized role
- Invalid token/input
- Duplicate/retry/idempotency case if relevant
- Offline/sync case if relevant

## 9. Commit discipline

Prefer small commits by vertical slice:

```text
feat(parking): implement session check-in and QR lookup
feat(payment): add simulation payment order and idempotency
feat(offline): add staff offline event queue
```

Do not mix unrelated services in one change unless the vertical slice requires it.

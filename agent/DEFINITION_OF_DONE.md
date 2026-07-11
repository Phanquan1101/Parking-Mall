# DEFINITION_OF_DONE.md — ParkFlow Mall

A task is only considered done when all applicable items below are satisfied.

## 1. Functional completion

- The requested business behavior works end-to-end.
- The implementation follows approved BRD/PRD/SAD scope.
- No out-of-scope feature was added.
- Relevant business rules are enforced.
- Edge cases are handled with clear error messages.

## 2. Security and authorization

- Backend enforces role-based access.
- Public endpoints expose only safe data.
- Tokens are not predictable.
- QR Lookup Token is not treated as Exit Pass.
- Manual override requires reason and audit log.
- Secrets are not committed.

## 3. Payment safety

For payment-related tasks:

- Simulation Mode works before external payment.
- Duplicate webhook/simulation does not double-pay.
- Amount and payment code are validated.
- Payment state changes are auditable.
- Unmatched payment goes to manual review.

## 4. Offline safety

For offline-related tasks:

- UI shows Online/Offline/Syncing.
- Offline events have event IDs.
- Local queue persists until sync.
- Sync endpoint is idempotent.
- Conflicts are visible.
- Server remains final source of truth.

## 5. Tests

At minimum, include or update tests covering:

- Happy path
- Invalid input
- Unauthorized role
- Business rule violation
- Duplicate/idempotency case
- Offline/sync case where relevant
- Audit log case where relevant

If tests cannot be run, document why and what manual verification was done.

## 6. Documentation

Update affected docs:

- API contract updated for new/changed endpoints
- Database schema updated for new/changed tables
- Test matrix updated for new business cases
- Decision log updated for assumptions/decisions

## 7. Code quality

- Code compiles.
- No obvious dead code.
- No hardcoded secrets.
- Environment variables documented.
- Naming is consistent with domain language.
- Controllers are thin; business logic is in services/domain.

## 8. Runtime quality

- Service starts locally.
- Health endpoint works.
- Docker config updated if service/env changes.
- Logs are useful for debugging but do not leak secrets.

## 9. Completion report

Each completed task must report:

```text
What changed:
Files changed:
Business rules implemented:
Tests run:
Known limitations:
Next recommended task:
```

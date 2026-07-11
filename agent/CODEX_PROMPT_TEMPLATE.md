# CODEX_PROMPT_TEMPLATE.md — How to Ask Agent Codex to Work

Use this template for every coding request.

```text
Implement [Slice/Task ID]: [Task Name]

Read first:
- AGENTS.md
- agent/PLAN.md
- agent/TASKS.md
- agent/CODING_RULES.md
- agent/DEFINITION_OF_DONE.md
- agent/VERTICAL_SLICE_ROADMAP.md
- docs/API_CONTRACT.md
- docs/DATABASE_SCHEMA.md
- docs/TEST_MATRIX.md
- docs/DECISION_LOG.md

Scope:
- [Allowed behavior 1]
- [Allowed behavior 2]
- [Allowed behavior 3]

Out of scope:
- [Forbidden behavior 1]
- [Forbidden behavior 2]

Business rules to enforce:
- [BR-xxx]
- [BR-xxx]

Before coding:
1. Restate your understanding.
2. List files you plan to change.
3. List tests you will add/run.

Definition of Done:
- [Specific acceptance criterion]
- [Specific acceptance criterion]
- Docs updated if API/schema/test behavior changes.
- Tests pass or test limitations are reported.
```

## Example prompt — Payment Simulation

```text
Implement Slice 3: Payment Simulation Mode.

Read first:
- AGENTS.md
- agent/PLAN.md
- agent/TASKS.md
- agent/CODING_RULES.md
- agent/DEFINITION_OF_DONE.md
- docs/API_CONTRACT.md
- docs/DATABASE_SCHEMA.md
- docs/TEST_MATRIX.md
- docs/DECISION_LOG.md

Scope:
- Create payment-service skeleton.
- Add payment order entity/table.
- Create payment order endpoint for PARKING_SESSION.
- Add simulation webhook endpoint.
- Enforce idempotency using providerTransactionId or idempotencyKey.
- Update Parking session paymentStatus to PAID and set gracePeriodUntil.
- Add audit log for payment update.

Out of scope:
- Do not integrate SePay Live.
- Do not implement refund.
- Do not implement reservation payment yet.

Business rules to enforce:
- Payment Simulation First Rule.
- Payment Webhook Idempotency Rule.
- Payment Matching Rule.
- Payment Reconciliation Rule, only prepare pending state if needed.

Before coding:
1. Restate your understanding.
2. List files to create/modify.
3. List tests to add/run.

Definition of Done:
- Payment order can be created.
- Simulated webhook marks order PAID.
- Duplicate simulation does not double-pay.
- Session payment state updates correctly.
- API_CONTRACT.md and TEST_MATRIX.md are updated if needed.
```

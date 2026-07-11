# Non-Functional Requirements

These targets apply to MVP/demo validation and are not production enterprise SLAs.

## Performance

- Create parking session after staff confirmation: P95 <= 2 seconds, excluding OCR.
- Ticket lookup API: P95 <= 2 seconds.
- Online check-out validation: P95 <= 2 seconds.
- OCR processing: P95 <= 5 seconds per image; timeout falls back to manual entry.
- Dashboard update after an online event: <= 5 seconds in demo conditions via polling or refresh.

## Capacity

- 3-5 concurrent staff users.
- 20 concurrent customer ticket views.
- 300-500 active sessions in seeded/demo data.
- 100-300 payment webhook/simulation events in test data.
- 100 merchant validations in test data.

## Availability & Offline Resilience

- Staff Console detects and displays online/offline state.
- Offline check-in events are queued locally.
- At least 95% of queued events should sync within 5 minutes after connectivity returns in demo conditions.
- OCR, payment, or dashboard failure must not crash the whole system.

## Security

- QR Lookup Tokens are opaque and difficult to guess.
- Dynamic Exit Pass default TTL is 60 seconds and each pass is one-time use.
- Protected APIs require JWT and role authorization.
- Public ticket APIs expose minimal safe data only.
- Vehicle images are not public by default.

## Data Consistency

- Payment simulation and webhook processing are idempotent.
- Offline synchronization is idempotent.
- Server state is authoritative after sync.
- Pending, conflict, mismatched, and reconciliation states are visible.
- Reconciliation should run every 1-5 minutes in demo/test environments.

## Usability

- Customers do not need an account to view a ticket.
- Merchant validation has no more than three main steps.
- Staff clearly see Online/Offline/Syncing state.
- Error messages include the next action.

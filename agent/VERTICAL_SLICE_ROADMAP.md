# VERTICAL_SLICE_ROADMAP.md - ParkFlow Mall

Vertical slices prove one workflow at a time. Audit/Fraud is cross-cutting and is included in relevant slices; it is not a separate slice. SePay Live is out of MVP.

## Slice 0 - Repository Foundation

Purpose: project folders, documentation structure, `.env.example`, Docker placeholder, and conventions only. No business logic, services, or frontend implementation.

Done when the repository structure and documentation contracts are clear.

## Slice 1 - API Gateway + Identity Skeleton

Purpose: gateway/identity skeleton, login placeholder, role model, and health checks.

Done when canonical auth paths, roles, and authorization behavior are documented.

## Slice 2 - Parking Session + QR Lookup

Purpose: parking session core, check-in draft, opaque QR Lookup Token, and public ticket lookup. QR Lookup cannot authorize exit.

Done when session states, safe lookup data, and plate confirmation rules are documented.

## Slice 3 - Customer Ticket Page

Purpose: customer page shows session summary, fee estimate, and payment status without implementing payment logic.

Done when mobile-safe ticket data and invalid-token behavior are documented.

## Slice 4 - Payment Simulation

Purpose: payment order, simulation success, `Idempotency-Key` header, amount/code matching, and payment status update. SePay Live is disabled.

Done when simulation retry and mismatch behavior are documented.

## Slice 5 - Dynamic Exit Pass + Check-out

Purpose: short-lived one-time Exit Pass, paid/zero-fee eligibility, plate match, grace period, and secure check-out.

Done when QR Lookup alone cannot authorize exit and manual override requires reason plus audit.

## Slice 6 - Offline Staff Mode

Purpose: Online/Offline/Syncing status, local cache, offline queue, canonical sync API, and visible conflicts.

Done when server-source-of-truth and limited offline exit rules are documented.

## Slice 7 - Merchant Invoice Aggregation

Purpose: online merchant validation, multiple invoices per session, default `AGGREGATE_INVOICE`, duplicate rejection, and discount recalculation.

Done when tenant ownership and one-invoice-one-use are documented.

## Slice 8 - Payment Reconciliation

Purpose: pending payment detection, webhook/simulation reconciliation, mismatched payment handling, and session/payment consistency.

Done when reconciliation jobs, states, and audit/dashboard visibility are documented.

## Slice 9 - Reservation Basic

Purpose: reservation, slot hold, payment state, check-in window, and `PENDING_RECONCILIATION`.

Reservation is Should Have and must not block core MVP. Refund and live payment are out of MVP.

## Slice 10 - OCR Assist

Purpose: vision-service contract, plate candidate, confidence score, staff confirmation/correction, and manual fallback.

OCR is Should Have and assistive only; it must never block check-in/check-out.

## Slice 11 - Dashboard

Purpose: active sessions, confirmed revenue, pending/mismatched payment, merchant validations, offline pending sync, and suspicious/conflict cases.

Done when confirmed and uncertain states are clearly separated.

## Slice 12 - Docker + Deploy Hardening

Purpose: Docker Compose completion, Railway/Supabase/hybrid deployment documentation, environment variables, and demo readiness.

Done when local and hybrid deployment paths are documented without hardcoded provider values.

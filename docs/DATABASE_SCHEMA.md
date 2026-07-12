# DATABASE_SCHEMA.md - ParkFlow Mall Database Schema Draft

This is a draft schema contract. MVP uses one Supabase PostgreSQL project with schema separation by service. No migrations are created by this document.

## 1. Naming and consistency rules

- Primary keys are UUIDs or ULID-style IDs.
- Public codes and tokens are opaque and are not database IDs.
- Timestamps use timezone-aware types.
- Status values use the canonical enums below.
- Services must not directly modify another service's schema.
- Retryable mutations use the HTTP `Idempotency-Key` header.

## 2. identity_schema

### users

`id` PK, `username` unique, `password_hash`, `display_name`, `status`, `created_at`, `updated_at`.

### roles

`id` PK, `code` unique (`ADMIN`, `PARKING_STAFF`, `MERCHANT_STAFF`), `name`.

### user_roles

`user_id` FK users, `role_id` FK roles, composite primary key (`user_id`, `role_id`).

## 3. parking_schema

### parking_sessions

`id` PK, `session_code` unique, `vehicle_plate`, `vehicle_plate_masked`, `vehicle_type`, `entry_time`, `exit_time`, `entry_gate_code`, `exit_gate_code`, `zone_code`, `entry_image_url`, `exit_image_url`, `entry_plate_confidence`, `exit_plate_confidence`, `status`, `payment_status`, `total_fee`, `discount_amount`, `final_fee`, `grace_period_until`, `reservation_id`, `created_by`, `created_at`, `updated_at`.

Canonical parking session statuses:

`ACTIVE`, `PENDING_PAYMENT`, `PAID`, `EXITED`, `SUSPICIOUS`, `LOST_QR`, `OFFLINE_PENDING_SYNC`, `CONFLICT`, `CANCELLED`.

### qr_lookup_tokens

`id` PK, `session_id` FK parking_sessions, `token_hash`, `status`, `created_at`, `expires_at`, `last_used_at`.

The raw lookup token is not stored when hashing is feasible.

### exit_passes

`id` PK, `session_id` FK parking_sessions, `token_hash`, `status`, `created_at`, `expires_at`, `used_at`, `invalidated_at`, `created_from`, `ttl_seconds`, `used_by`.

Exit Pass statuses: `ACTIVE`, `USED`, `EXPIRED`, `INVALIDATED`.

Slice 5 uses an in-memory repository only; this remains the persistence contract for a later migration. An active pass is invalidated when a replacement pass is created for the same session.

### vehicles

`id` PK, `normalized_plate` unique per parking lot, `vehicle_type`, `created_at`, `updated_at`.

### gates

`id` PK, `gate_code` unique, `gate_type`, `zone_code`, `status`.

### parking_zones

`id` PK, `zone_code` unique, `name`, `capacity`, `status`.

### offline_events

`id` PK, `event_id` unique, `device_id`, `staff_id`, `event_type`, `occurred_at`, `received_at`, `idempotency_key` unique, `payload_json`, `sync_status`, `server_resource_id`, `conflict_reason`.

Sync statuses: `PENDING`, `SYNCING`, `ACCEPTED`, `DUPLICATE`, `REJECTED`, `CONFLICT`, `MANUAL_REVIEW_REQUIRED`.

### sync_conflicts

`id` PK, `offline_event_id` FK offline_events, `resource_type`, `resource_id`, `conflict_type`, `description`, `status`, `created_at`, `resolved_at`, `resolved_by`.

### fraud_alerts

`id` PK, `alert_type`, `severity`, `target_type`, `target_id`, `description`, `status`, `created_at`, `resolved_at`, `resolved_by`.

### parking_audit_logs

`id` PK, `actor_id`, `actor_role`, `action`, `target_type`, `target_id`, `old_value_json`, `new_value_json`, `reason` nullable except when a manual override is recorded, `created_at`.

## 4. payment_schema

### payment_orders

`id` PK, `payment_code` unique, `target_type`, `target_id`, `amount`, `status`, `provider_mode`, `provider`, `provider_transaction_id` nullable/unique when present, `paid_at`, `expires_at`, `created_at`, `updated_at`.

Canonical payment statuses:

`PENDING`, `PAID`, `FAILED`, `EXPIRED`, `MISMATCHED`, `PENDING_RECONCILIATION`.

MVP provider modes are `SIMULATION` and `SEPAY_TEST`; `SEPAY_LIVE` remains disabled.

### payment_transactions

`id` PK, `payment_order_id` FK payment_orders, `provider_transaction_id` unique, `amount`, `transaction_time`, `raw_description`, `status`, `idempotency_key`, `created_at`.

### webhook_events

`id` PK, `provider`, `provider_event_id`, `idempotency_key` unique where present, `raw_payload`, `processing_status`, `error_message`, `created_at`, `processed_at`.

### payment_reconciliation_jobs

`id` PK, `started_at`, `completed_at`, `status`, `checked_count`, `resolved_count`, `manual_review_count`, `error_message`.

### payment_reconciliation_items

`id` PK, `job_id` FK payment_reconciliation_jobs, `payment_order_id` FK payment_orders, `previous_status`, `result_status`, `reason`, `created_at`.

## 5. merchant_schema

### tenants

`id` PK, `tenant_code` unique, `tenant_name`, `status`, `created_at`.

### merchant_staff_profiles

`id` PK, `user_id`, `tenant_id` FK tenants, `status`, `created_at`.

### invoice_validations

`id` PK, `session_id`, `tenant_id` FK tenants, `invoice_code` globally unique, `invoice_amount`, `validation_status`, `discount_applied`, `validated_by`, `validated_at`, `created_at`.

### discount_rules

`id` PK, `policy_code`, `policy_type` (`AGGREGATE_INVOICE` default), `threshold_amount`, `discount_type`, `free_minutes`, `fixed_amount`, `status`, `created_at`.

### discount_applications

`id` PK, `session_id`, `rule_id` FK discount_rules, `eligible_invoice_total`, `discount_amount`, `final_fee`, `created_at`.

### merchant_audit_logs

`id` PK, `actor_id`, `action`, `target_type`, `target_id`, `reason`, `created_at`.

## 6. reservation_schema

### reservations

`id` PK, `reservation_code` unique, `vehicle_plate`, `vehicle_type`, `zone_code`, `reserved_start_time`, `reserved_end_time`, `checkin_window_start`, `checkin_window_end`, `status`, `payment_order_id`, `slot_hold_id`, `created_at`, `updated_at`.

Canonical reservation statuses:

`DRAFT`, `PENDING_PAYMENT`, `CONFIRMED`, `CHECKED_IN`, `EXPIRED`, `NO_SHOW`, `CANCELLED`, `PENDING_RECONCILIATION`.

### reservation_slot_holds

`id` PK, `reservation_id` FK reservations, `zone_code`, `hold_status`, `hold_expires_at`, `created_at`.

### reservation_policies

`id` PK, `policy_code`, `payment_window_minutes`, `checkin_window_minutes`, `status`, `created_at`.

### reservation_reconciliation_items

`id` PK, `reservation_id` FK reservations, `payment_order_id`, `previous_status`, `result_status`, `reason`, `created_at`, `resolved_at`, `resolved_by`.

## 7. reporting_schema

Optional MVP read models:

- `dashboard_snapshots`: `id`, `snapshot_time`, operational counters, `created_at`.
- `reporting_read_models`: query-optimized projections with source version and refresh time.

## 8. Required indexes and constraints

- Unique active normalized plate per parking lot for statuses `ACTIVE`, `PENDING_PAYMENT`, `PAID`, `SUSPICIOUS`.
- Index lookup token hash, session status, payment status, payment code, invoice code, event ID, and reconciliation status.
- Unique `invoice_code` globally for MVP.
- Unique offline `event_id` and idempotency key where applicable.

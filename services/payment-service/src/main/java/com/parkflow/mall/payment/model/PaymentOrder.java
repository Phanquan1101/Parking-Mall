package com.parkflow.mall.payment.model;
import java.time.Instant;
public record PaymentOrder(String id, String paymentCode, PaymentTargetType targetType, String targetId, long amount, String currency, PaymentStatus status, Instant createdAt, Instant expiresAt, Instant paidAt, boolean simulationMode, String creationIdempotencyKey) {}

package com.parkflow.mall.payment.model;
import java.time.Instant;
public record PaymentTransaction(String id, String paymentOrderId, String provider, String providerEventId, long amount, String paymentCode, PaymentStatus status, Instant createdAt) {}

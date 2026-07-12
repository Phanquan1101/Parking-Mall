package com.parkflow.mall.merchant.model;
import java.time.Instant;
public record InvoiceValidation(String id, String sessionId, String sessionCode, String tenantId, String merchantUsername, String invoiceCode, long invoiceAmount, InvoiceValidationStatus status, Instant validatedAt) {}

package com.parkflow.mall.merchant.dto;
public record InvoiceValidationResponse(String validationId,String sessionId,String sessionCode,String tenantId,String invoiceCode,long invoiceAmount,long totalEligibleInvoiceAmount,String discountPolicy,long discountAmount,long finalFee,String status,String message) {}

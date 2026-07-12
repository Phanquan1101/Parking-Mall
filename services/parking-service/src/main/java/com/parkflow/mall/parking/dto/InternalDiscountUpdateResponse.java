package com.parkflow.mall.parking.dto;
public record InternalDiscountUpdateResponse(String sessionId, long estimatedFee, long discountAmount, long finalFee, long totalEligibleInvoiceAmount, boolean updated) {}

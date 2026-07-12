package com.parkflow.mall.parking.dto;
public record InternalDiscountUpdateRequest(String source, long totalEligibleInvoiceAmount, long discountAmount, String discountPolicy, String updatedBy) {}

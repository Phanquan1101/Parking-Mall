package com.parkflow.mall.parking.model;
public record MerchantDiscountState(long totalEligibleInvoiceAmount, long discountAmount, long finalFee, String discountPolicy) {}

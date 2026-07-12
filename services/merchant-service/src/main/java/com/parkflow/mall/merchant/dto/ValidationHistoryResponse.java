package com.parkflow.mall.merchant.dto;
import java.util.List;
public record ValidationHistoryResponse(String sessionId,String discountPolicy,long totalEligibleInvoiceAmount,long discountAmount,List<InvoiceValidationResponse> validations) {}

package com.parkflow.mall.merchant.dto;
public record ValidateInvoiceRequest(String lookupToken, String invoiceCode, long invoiceAmount) {}

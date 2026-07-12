package com.parkflow.mall.parking.dto;
import com.parkflow.mall.parking.model.PaymentStatus;
public record InternalPaymentUpdateResponse(String sessionId, PaymentStatus paymentStatus, Long amountPaid, boolean updated) {}

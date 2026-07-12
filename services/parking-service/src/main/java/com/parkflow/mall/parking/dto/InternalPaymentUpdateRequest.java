package com.parkflow.mall.parking.dto;
import com.parkflow.mall.parking.model.PaymentStatus;
import java.time.Instant;
public record InternalPaymentUpdateRequest(String paymentOrderId, PaymentStatus paymentStatus, long amountPaid, Instant paidAt) {}

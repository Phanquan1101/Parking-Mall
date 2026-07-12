package com.parkflow.mall.parking.dto;

import com.parkflow.mall.parking.model.PaymentStatus;
import java.time.Instant;

public record CheckOutResponse(
        String sessionId,
        String sessionCode,
        String status,
        PaymentStatus paymentStatus,
        Instant entryTime,
        Instant exitTime,
        String exitGate,
        String exitPlate,
        boolean manualOverride,
        String reason,
        String message) {
}

package com.parkflow.mall.parking.dto;

import com.parkflow.mall.parking.model.ParkingSessionStatus;
import com.parkflow.mall.parking.model.PaymentStatus;
import java.time.Instant;

public record ValidateExitPassResponse(
        boolean valid,
        String sessionId,
        String sessionCode,
        String vehiclePlate,
        String normalizedEntryPlate,
        String normalizedExitPlate,
        PaymentStatus paymentStatus,
        ParkingSessionStatus sessionStatus,
        Instant expiresAt,
        String message) {
}

package com.parkflow.mall.parking.dto;

import com.parkflow.mall.parking.model.ParkingSessionStatus;
import com.parkflow.mall.parking.model.PaymentStatus;
import com.parkflow.mall.parking.model.VehicleType;
import java.time.Instant;

public record PublicTicketResponse(
        String sessionId,
        String sessionCode,
        String vehiclePlate,
        VehicleType vehicleType,
        ParkingSessionStatus status,
        PaymentStatus paymentStatus,
        Instant entryTime,
        long durationMinutes,
        long estimatedFee,
        long discountAmount,
        long finalFee,
        String message) {
}

package com.parkflow.mall.parking.model;

import java.time.Instant;

public record ParkingSession(
        String id,
        String sessionCode,
        String vehiclePlate,
        String normalizedPlate,
        VehicleType vehicleType,
        ParkingSessionStatus status,
        PaymentStatus paymentStatus,
        Instant entryTime,
        String entryGate,
        String staffId,
        PlateSource plateSource,
        String qrLookupToken,
        String paymentOrderId,
        Long amountPaid,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt) {
}

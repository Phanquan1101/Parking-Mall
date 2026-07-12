package com.parkflow.mall.parking.dto;

import com.parkflow.mall.parking.model.ParkingSessionStatus;
import com.parkflow.mall.parking.model.PaymentStatus;
import com.parkflow.mall.parking.model.VehicleType;
import java.time.Instant;

public record ParkingSessionResponse(
        String sessionId,
        String sessionCode,
        String vehiclePlate,
        String normalizedPlate,
        VehicleType vehicleType,
        ParkingSessionStatus status,
        PaymentStatus paymentStatus,
        Instant entryTime,
        String entryGate,
        String qrLookupToken,
        String reservationId,
        String reservationCode,
        boolean createdFromReservation,
        String ocrRequestId,
        String ocrCandidatePlate,
        Double ocrConfidence,
        String ticketUrl) {
}

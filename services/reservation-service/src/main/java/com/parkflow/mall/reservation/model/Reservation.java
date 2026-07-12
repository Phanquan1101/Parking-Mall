package com.parkflow.mall.reservation.model;

import java.time.Instant;

public record Reservation(String id, String reservationCode, String vehiclePlate, String normalizedVehiclePlate,
        VehicleType vehicleType, String customerName, String customerPhone, Instant reservedFrom, Instant reservedUntil,
        Instant expiresAt, ReservationStatus status, Instant createdAt, Instant cancelledAt, String cancelReason,
        Instant consumedAt, String consumedByParkingSessionId, String consumeRequestId) { }

package com.parkflow.mall.reservation.dto;
import com.parkflow.mall.reservation.model.ReservationStatus;
import com.parkflow.mall.reservation.model.VehicleType;
import java.time.Instant;
public record ReservationResponse(String reservationId, String reservationCode, String vehiclePlate, VehicleType vehicleType, Instant reservedFrom, Instant reservedUntil, Instant expiresAt, ReservationStatus status, String message) { }

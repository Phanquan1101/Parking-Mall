package com.parkflow.mall.reservation.dto;
import com.parkflow.mall.reservation.model.VehicleType;
import java.time.Instant;
public record CreateReservationRequest(String vehiclePlate, VehicleType vehicleType, Instant reservedFrom, Instant reservedUntil, String customerName, String customerPhone) { }

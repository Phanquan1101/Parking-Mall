package com.parkflow.mall.reservation.dto;
import com.parkflow.mall.reservation.model.VehicleType;
public record ConsumeReservationRequest(String vehiclePlate, VehicleType vehicleType, String parkingSessionId, String checkInRequestId) { }

package com.parkflow.mall.parking.dto;

public record OfflineCheckInPayload(String vehiclePlate, String vehicleType, String entryGate, String plateSource) {
}

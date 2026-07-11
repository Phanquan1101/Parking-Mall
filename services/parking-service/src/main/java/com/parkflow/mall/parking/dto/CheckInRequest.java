package com.parkflow.mall.parking.dto;

import com.parkflow.mall.parking.model.PlateSource;
import com.parkflow.mall.parking.model.VehicleType;

public record CheckInRequest(
        String vehiclePlate,
        VehicleType vehicleType,
        String entryGate,
        String staffId,
        PlateSource plateSource) {
}

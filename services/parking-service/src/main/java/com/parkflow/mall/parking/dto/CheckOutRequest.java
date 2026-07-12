package com.parkflow.mall.parking.dto;

public record CheckOutRequest(String exitPassToken, String exitPlate, String exitGate) {
}

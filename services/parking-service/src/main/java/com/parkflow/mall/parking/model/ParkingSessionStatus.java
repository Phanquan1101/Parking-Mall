package com.parkflow.mall.parking.model;

public enum ParkingSessionStatus {
    ACTIVE,
    PENDING_PAYMENT,
    PAID,
    EXITED,
    SUSPICIOUS,
    LOST_QR,
    OFFLINE_PENDING_SYNC,
    CONFLICT,
    CANCELLED
}

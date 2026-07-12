package com.parkflow.mall.parking.model;

public enum OfflineSyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    DUPLICATE,
    REJECTED,
    CONFLICT,
    MANUAL_REVIEW_REQUIRED
}

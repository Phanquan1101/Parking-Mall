package com.parkflow.mall.parking.dto;

import com.parkflow.mall.parking.model.OfflineSyncStatus;

public record OfflineEventStatusResponse(String eventId, OfflineSyncStatus status, String serverSessionId, String sessionCode, String message) {
}

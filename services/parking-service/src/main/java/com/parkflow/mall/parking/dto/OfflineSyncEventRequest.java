package com.parkflow.mall.parking.dto;

import java.time.Instant;

public record OfflineSyncEventRequest(
        String eventId,
        String eventType,
        String idempotencyKey,
        Instant localTimestamp,
        OfflineCheckInPayload payload) {
}

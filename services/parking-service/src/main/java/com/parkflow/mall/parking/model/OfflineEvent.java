package com.parkflow.mall.parking.model;

import java.time.Instant;

public record OfflineEvent(
        String eventId,
        OfflineEventType eventType,
        String eventIdempotencyKey,
        String syncRequestIdempotencyKey,
        String deviceId,
        String staffSubject,
        Instant localTimestamp,
        Instant receivedAt,
        String vehiclePlate,
        String vehicleType,
        String entryGate,
        String plateSource,
        OfflineSyncStatus status,
        String serverSessionId,
        String sessionCode,
        String message,
        String conflictReason) {
}

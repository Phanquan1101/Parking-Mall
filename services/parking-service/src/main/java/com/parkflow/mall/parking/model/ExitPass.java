package com.parkflow.mall.parking.model;

import java.time.Instant;

public record ExitPass(
        String id,
        String token,
        String sessionId,
        ExitPassStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant usedAt,
        Instant invalidatedAt,
        String createdFrom,
        long ttlSeconds) {
}

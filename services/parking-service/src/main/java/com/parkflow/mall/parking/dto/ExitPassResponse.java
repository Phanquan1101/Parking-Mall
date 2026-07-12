package com.parkflow.mall.parking.dto;

import com.parkflow.mall.parking.model.ExitPassStatus;
import java.time.Instant;

public record ExitPassResponse(
        String exitPassToken,
        String sessionId,
        String sessionCode,
        Instant expiresAt,
        long ttlSeconds,
        ExitPassStatus status,
        String message) {
}

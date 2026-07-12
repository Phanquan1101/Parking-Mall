package com.parkflow.mall.parking.model;

import java.time.Instant;

/** Lightweight internal event for Slice 2; this is not the full Audit/Fraud module. */
public record ParkingSessionEvent(String type, String sessionId, String actorId, Instant occurredAt, String details) {
    public ParkingSessionEvent(String type, String sessionId, String actorId, Instant occurredAt) {
        this(type, sessionId, actorId, occurredAt, null);
    }
}

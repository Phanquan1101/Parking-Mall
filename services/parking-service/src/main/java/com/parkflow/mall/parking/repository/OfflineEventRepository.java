package com.parkflow.mall.parking.repository;

import com.parkflow.mall.parking.model.OfflineEvent;
import java.util.Optional;

public interface OfflineEventRepository {
    Optional<OfflineEvent> findByEventId(String eventId);

    Optional<OfflineEvent> findByEventIdempotencyKey(String idempotencyKey);

    void save(OfflineEvent event);
}

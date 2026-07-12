package com.parkflow.mall.parking.repository;

import com.parkflow.mall.parking.model.OfflineEvent;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryOfflineEventRepository implements OfflineEventRepository {
    private final Map<String, OfflineEvent> eventsById = new ConcurrentHashMap<>();
    private final Map<String, String> eventIdByIdempotencyKey = new ConcurrentHashMap<>();

    @Override
    public Optional<OfflineEvent> findByEventId(String eventId) {
        return Optional.ofNullable(eventsById.get(eventId));
    }

    @Override
    public Optional<OfflineEvent> findByEventIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(eventIdByIdempotencyKey.get(idempotencyKey)).flatMap(this::findByEventId);
    }

    @Override
    public synchronized void save(OfflineEvent event) {
        eventsById.put(event.eventId(), event);
        eventIdByIdempotencyKey.put(event.eventIdempotencyKey(), event.eventId());
    }
}

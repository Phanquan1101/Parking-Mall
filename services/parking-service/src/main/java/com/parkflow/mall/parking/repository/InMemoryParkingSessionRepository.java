package com.parkflow.mall.parking.repository;

import com.parkflow.mall.parking.model.ParkingSession;
import com.parkflow.mall.parking.model.ParkingSessionStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryParkingSessionRepository implements ParkingSessionRepository {
    // TODO: Replace with persistent parking_schema storage after the database slice is approved.
    private final Map<String, ParkingSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdByToken = new ConcurrentHashMap<>();

    @Override
    public synchronized boolean saveIfNoActive(ParkingSession session) {
        if (existsActiveByNormalizedPlate(session.normalizedPlate())) {
            return false;
        }
        sessionsById.put(session.id(), session);
        sessionIdByToken.put(session.qrLookupToken(), session.id());
        return true;
    }

    @Override
    public boolean existsActiveByNormalizedPlate(String normalizedPlate) {
        return sessionsById.values().stream()
                .anyMatch(session -> session.normalizedPlate().equals(normalizedPlate)
                        && session.status() == ParkingSessionStatus.ACTIVE);
    }

    @Override
    public Optional<ParkingSession> findById(String id) {
        return Optional.ofNullable(sessionsById.get(id));
    }

    @Override
    public Optional<ParkingSession> findByLookupToken(String lookupToken) {
        return Optional.ofNullable(sessionIdByToken.get(lookupToken)).flatMap(this::findById);
    }

    @Override
    public List<ParkingSession> search(ParkingSessionStatus status, String normalizedPlate) {
        return sessionsById.values().stream()
                .filter(session -> status == null || session.status() == status)
                .filter(session -> normalizedPlate == null || session.normalizedPlate().equals(normalizedPlate))
                .sorted(Comparator.comparing(ParkingSession::entryTime).reversed())
                .toList();
    }

    @Override
    public void update(ParkingSession session) { sessionsById.put(session.id(), session); }
}

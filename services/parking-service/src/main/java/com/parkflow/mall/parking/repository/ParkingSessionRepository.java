package com.parkflow.mall.parking.repository;

import com.parkflow.mall.parking.model.ParkingSession;
import com.parkflow.mall.parking.model.ParkingSessionStatus;
import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository {
    boolean saveIfNoActive(ParkingSession session);

    boolean existsActiveByNormalizedPlate(String normalizedPlate);

    Optional<ParkingSession> findById(String id);

    Optional<ParkingSession> findByLookupToken(String lookupToken);

    List<ParkingSession> search(ParkingSessionStatus status, String normalizedPlate);

    void update(ParkingSession session);
}

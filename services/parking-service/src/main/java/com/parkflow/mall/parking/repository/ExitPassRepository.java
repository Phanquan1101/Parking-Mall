package com.parkflow.mall.parking.repository;

import com.parkflow.mall.parking.model.ExitPass;
import java.util.Optional;

public interface ExitPassRepository {
    void save(ExitPass exitPass);

    Optional<ExitPass> findByToken(String token);

    Optional<ExitPass> findActiveBySessionId(String sessionId);

    void invalidateActiveForSession(String sessionId);
}

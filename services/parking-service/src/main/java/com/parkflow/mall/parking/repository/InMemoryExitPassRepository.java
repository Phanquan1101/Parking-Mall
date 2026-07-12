package com.parkflow.mall.parking.repository;

import com.parkflow.mall.parking.model.ExitPass;
import com.parkflow.mall.parking.model.ExitPassStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryExitPassRepository implements ExitPassRepository {
    private final Map<String, ExitPass> passesByToken = new ConcurrentHashMap<>();

    @Override
    public void save(ExitPass exitPass) {
        passesByToken.put(exitPass.token(), exitPass);
    }

    @Override
    public Optional<ExitPass> findByToken(String token) {
        return Optional.ofNullable(passesByToken.get(token));
    }

    @Override
    public Optional<ExitPass> findActiveBySessionId(String sessionId) {
        return passesByToken.values().stream()
                .filter(pass -> pass.sessionId().equals(sessionId) && pass.status() == ExitPassStatus.ACTIVE)
                .filter(pass -> pass.expiresAt().isAfter(Instant.now()))
                .findFirst();
    }

    @Override
    public void invalidateActiveForSession(String sessionId) {
        Instant now = Instant.now();
        passesByToken.replaceAll((token, pass) -> pass.sessionId().equals(sessionId)
                        && pass.status() == ExitPassStatus.ACTIVE
                ? new ExitPass(pass.id(), pass.token(), pass.sessionId(), ExitPassStatus.INVALIDATED,
                        pass.createdAt(), pass.expiresAt(), pass.usedAt(), now, pass.createdFrom(), pass.ttlSeconds())
                : pass);
    }
}

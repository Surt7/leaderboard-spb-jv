package com.worldpinball.leaderboard.application.services;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface MachineStatusCacheService {

    Optional<String> getStatus(UUID machineId);

    void setStatus(UUID machineId, String status, Duration ttl);

    void invalidateStatus(UUID machineId);
}

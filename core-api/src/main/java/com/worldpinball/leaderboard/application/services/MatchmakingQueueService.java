package com.worldpinball.leaderboard.application.services;

import java.util.Optional;
import java.util.UUID;

public interface MatchmakingQueueService {
    void enqueue(String machineId, UUID playerId);
    boolean remove(String machineId, UUID playerId);
    Optional<UUID> dequeueNext(String machineId);
}

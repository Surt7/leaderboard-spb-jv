package com.worldpinball.leaderboard.application.services;

import java.util.UUID;
import java.util.function.Consumer;

public interface LobbyNotificationService {
    void notifyLobbyUpdated(UUID sessionId);
    void notifySessionLaunched(UUID sessionId);
    void notifySessionAborted(UUID sessionId);
    void notifyScoreSubmitted(UUID sessionId, int slotNumber, long score);
    void notifyMatchFound(UUID sessionId, UUID machineId);

    Runnable subscribe(UUID sessionId, Consumer<UUID> consumer);
    Runnable subscribeAll(Consumer<UUID> consumer);
}

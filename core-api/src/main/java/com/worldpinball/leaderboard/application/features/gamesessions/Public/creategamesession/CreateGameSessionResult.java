package com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession;

import java.time.Instant;
import java.util.UUID;

public sealed interface CreateGameSessionResult {

    record Success(
        UUID sessionId,
        String nonce,
        Instant createdAtUtc
    ) implements CreateGameSessionResult {}

    record DeviceNotFound(UUID deviceId) implements CreateGameSessionResult {}

    record DeviceNotActive(UUID deviceId) implements CreateGameSessionResult {}

    record MachineNotAttached(UUID deviceId) implements CreateGameSessionResult {}

    record ModeNotFound(String modeCode) implements CreateGameSessionResult {}
}

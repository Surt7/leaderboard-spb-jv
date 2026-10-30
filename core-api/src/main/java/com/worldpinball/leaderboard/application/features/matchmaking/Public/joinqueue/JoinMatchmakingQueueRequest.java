package com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record JoinMatchmakingQueueRequest(
        @NotNull(message = "MachineId cannot be null.")
        UUID machineId,

        @NotNull(message = "PlayerId cannot be null.")
        UUID playerId
) {}

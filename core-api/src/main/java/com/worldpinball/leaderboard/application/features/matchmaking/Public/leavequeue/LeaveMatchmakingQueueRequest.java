package com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record LeaveMatchmakingQueueRequest(
        @NotNull(message = "MachineId cannot be null.")
        UUID machineId,

        @NotNull(message = "PlayerId cannot be null.")
        UUID playerId
) {}

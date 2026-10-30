package com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue;

import java.util.UUID;

public sealed interface JoinMatchmakingQueueResult {
    record Success() implements JoinMatchmakingQueueResult {}
    record MachineNotFound(UUID machineId) implements JoinMatchmakingQueueResult {}
}

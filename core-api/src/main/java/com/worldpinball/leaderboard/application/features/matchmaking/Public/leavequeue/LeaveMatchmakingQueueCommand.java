package com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue;

import java.util.UUID;

public record LeaveMatchmakingQueueCommand(UUID machineId, UUID playerId) {}

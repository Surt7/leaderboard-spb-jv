package com.worldpinball.leaderboard.application.features.matchmaking.Public.matchnextplayer;

import java.util.UUID;

public record MatchNextPlayerCommand(UUID sessionId, UUID machineId) {}

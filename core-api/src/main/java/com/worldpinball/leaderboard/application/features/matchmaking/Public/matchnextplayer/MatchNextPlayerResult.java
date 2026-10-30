package com.worldpinball.leaderboard.application.features.matchmaking.Public.matchnextplayer;

import java.util.UUID;

public sealed interface MatchNextPlayerResult {
    record Success(UUID playerId, int slotNumber, boolean matchFound) implements MatchNextPlayerResult {}
    record SessionNotFound(UUID sessionId) implements MatchNextPlayerResult {}
    record SessionFull(UUID sessionId) implements MatchNextPlayerResult {}
    record NoPlayerInQueue() implements MatchNextPlayerResult {}
}

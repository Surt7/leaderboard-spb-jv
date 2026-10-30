package com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession;

import java.util.UUID;

public sealed interface AbortSessionResult {
    record Success(UUID sessionId) implements AbortSessionResult {}
    record SessionNotFound(UUID sessionId) implements AbortSessionResult {}
    record CannotAbortSession(String reason) implements AbortSessionResult {}
}

package com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession;

import java.util.UUID;

public sealed interface LaunchSessionResult {
    record Success(UUID sessionId) implements LaunchSessionResult {}
    record SessionNotFound(UUID sessionId) implements LaunchSessionResult {}
    record CannotStartSession(String message) implements LaunchSessionResult {}
}

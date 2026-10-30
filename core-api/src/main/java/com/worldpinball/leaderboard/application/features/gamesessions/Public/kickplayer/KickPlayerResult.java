package com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer;

import java.util.UUID;

public sealed interface KickPlayerResult {
    record Success(UUID sessionId, int slotNumber) implements KickPlayerResult {}
    record SessionNotFound(UUID sessionId) implements KickPlayerResult {}
    record InvalidSlot(int slotNumber) implements KickPlayerResult {}
    record PlayerNotFound(int slotNumber) implements KickPlayerResult {}
    record CannotRemovePlayerActiveSession(UUID sessionId) implements KickPlayerResult {}
}

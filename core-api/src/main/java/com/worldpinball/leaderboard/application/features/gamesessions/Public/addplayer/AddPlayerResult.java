package com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer;

import java.util.UUID;

public sealed interface AddPlayerResult {
    record Success(UUID sessionId, int slotNumber) implements AddPlayerResult {}
    record SessionNotFound(UUID sessionId) implements AddPlayerResult {}
    record SessionNotJoinable(UUID sessionId) implements AddPlayerResult {}
    record SlotAlreadyOccupied(int slotNumber) implements AddPlayerResult {}
    record UserAlreadyRegistered(UUID userId) implements AddPlayerResult {}
    record InvalidSlot(int slotNumber) implements AddPlayerResult {}
    record RegisteredUserRequired(UUID sessionId) implements AddPlayerResult {}
}

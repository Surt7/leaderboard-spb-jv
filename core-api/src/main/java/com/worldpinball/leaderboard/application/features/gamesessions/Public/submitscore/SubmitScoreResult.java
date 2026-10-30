package com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore;

import java.util.List;
import java.util.UUID;

public sealed interface SubmitScoreResult {
    record Success(List<UUID> scoreEntryIds) implements SubmitScoreResult {}
    record SessionNotFound(UUID sessionId) implements SubmitScoreResult {}
    record SessionAlreadyEnded(UUID sessionId) implements SubmitScoreResult {}
    record SessionNotStarted(UUID sessionId) implements SubmitScoreResult {}
    record SlotNotFound(int slotNumber) implements SubmitScoreResult {}
    record SlotAlreadyOccupied(int slotNumber) implements SubmitScoreResult {}
    record DeviceMismatch(UUID expectedDeviceId, UUID actualDeviceId) implements SubmitScoreResult {}
    record EmptyScores() implements SubmitScoreResult {}
}

package com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore;

import java.util.UUID;

public sealed interface RecordBallScoreResult {
    record Success(UUID sessionId, int slotNumber, int ballNumber, long score) implements RecordBallScoreResult {}
    record SessionNotFound(UUID sessionId) implements RecordBallScoreResult {}
    record DeviceMismatch(UUID expectedDeviceId, UUID actualDeviceId) implements RecordBallScoreResult {}
    record CannotRecordBallScore(String reason) implements RecordBallScoreResult {}
}

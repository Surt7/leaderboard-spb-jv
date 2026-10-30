package com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore.commands;

import java.util.UUID;

public record RecordBallScoreCommand(
    UUID sessionId,
    UUID deviceId,
    int slotNumber,
    int ballNumber,
    long score
) {}

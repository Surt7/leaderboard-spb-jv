package com.worldpinball.leaderboard.application.features.leaderboard.Admin.dashboard;

import java.time.Instant;
import java.util.UUID;

public record MachineDashboardDto(
        UUID machineId,
        String serialNumber,
        int totalGamesPlayed,
        long highScore,
        long averageScore,
        int totalUniquePlayers,
        Instant lastPlayedAtUtc
) {}

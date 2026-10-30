package com.worldpinball.leaderboard.application.features.leaderboard.Public.dtos;

import java.time.Instant;
import java.util.UUID;

public record LeaderboardRowDto(
    long rank,
    UUID scoreEntryId,
    String playerInitials,
    String playerName,
    long score,
    Instant achievedAtUtc,
    String ipName,
    String modeName
) {
}

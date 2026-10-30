package com.worldpinball.leaderboard.application.features.leaderboard.Public.dtos;

import java.util.UUID;

public record LeaderboardEntryDto(
        UUID scoreEntryId,
        long score,
        long rank
) {
}

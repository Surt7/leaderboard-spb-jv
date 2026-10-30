package com.worldpinball.leaderboard.application.features.leaderboard.Public.getleaderboard;

import java.util.UUID;

public record GetLeaderboardQuery(
    UUID ipId,
    UUID modeId,
    String ipSlug,
    String modeCode,
    int limit
) {
    public GetLeaderboardQuery {
        if (limit <= 0) {
            limit = 50;
        }
        if (limit > 100) {
            limit = 100;
        }
    }
}

package com.worldpinball.leaderboard.application.features.leaderboard.Public.getplayerrank;

import java.util.UUID;

public record GetPlayerRankQuery(
    UUID ipId,
    UUID modeId,
    long score
) {
    public GetPlayerRankQuery {
        if (ipId == null) {
            throw new IllegalArgumentException("IpId cannot be null.");
        }
        if (modeId == null) {
            throw new IllegalArgumentException("ModeId cannot be null.");
        }
        if (score < 0) {
            throw new IllegalArgumentException("Score cannot be negative.");
        }
    }
}

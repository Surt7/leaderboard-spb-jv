package com.worldpinball.leaderboard.application.features.leaderboard.Public.getplayerrank;

public sealed interface GetPlayerRankResult {
    record Success(long rank, long score) implements GetPlayerRankResult {}
    record InvalidParameters(String message) implements GetPlayerRankResult {}
}

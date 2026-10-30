package com.worldpinball.leaderboard.application.features.leaderboard.Public.getleaderboard;

import com.worldpinball.leaderboard.application.features.leaderboard.Public.dtos.LeaderboardRowDto;

import java.util.List;

public sealed interface GetLeaderboardResult {
    record Success(List<LeaderboardRowDto> rows) implements GetLeaderboardResult {}
    record InvalidParameters(String message) implements GetLeaderboardResult {}
}

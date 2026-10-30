package com.worldpinball.leaderboard.application.services;

import com.worldpinball.leaderboard.application.features.leaderboard.Public.dtos.LeaderboardEntryDto;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.dtos.LeaderboardRowDto;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RealtimeLeaderboardService {

    void addScore(UUID ipId, UUID modeId, UUID scoreEntryId, long score);

    List<LeaderboardEntryDto> getTopScores(UUID ipId, UUID modeId, int limit);

    Optional<Long> getRank(UUID ipId, UUID modeId, UUID scoreEntryId);

    Optional<List<LeaderboardRowDto>> getCachedLeaderboard(UUID ipId, UUID modeId, int limit);

    void setCachedLeaderboard(UUID ipId, UUID modeId, int limit, List<LeaderboardRowDto> rows, Duration ttl);

    void invalidateCachedLeaderboard(UUID ipId, UUID modeId);
}

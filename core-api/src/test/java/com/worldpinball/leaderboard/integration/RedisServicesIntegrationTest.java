package com.worldpinball.leaderboard.integration;

import com.worldpinball.leaderboard.application.features.leaderboard.Public.dtos.LeaderboardEntryDto;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.dtos.LeaderboardRowDto;
import com.worldpinball.leaderboard.application.services.LiveTelemetryService;
import com.worldpinball.leaderboard.application.services.MachineStatusCacheService;
import com.worldpinball.leaderboard.application.services.MatchmakingQueueService;
import com.worldpinball.leaderboard.application.services.RealtimeLeaderboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisServicesIntegrationTest {

    @Autowired
    private RealtimeLeaderboardService realtimeLeaderboardService;

    @Autowired
    private MatchmakingQueueService matchmakingQueueService;

    @Autowired
    private MachineStatusCacheService machineStatusCacheService;

    @Autowired
    private LiveTelemetryService liveTelemetryService;

    @Test
    void shouldRankScoresInRedisZSetAndCacheLeaderboardDtos() {
        UUID ipId = UUID.randomUUID();
        UUID modeId = UUID.randomUUID();

        UUID scoreLow = UUID.randomUUID();
        UUID scoreHigh = UUID.randomUUID();
        UUID scoreMid = UUID.randomUUID();

        realtimeLeaderboardService.addScore(ipId, modeId, scoreLow, 1_500_000L);
        realtimeLeaderboardService.addScore(ipId, modeId, scoreHigh, 95_000_000L);
        realtimeLeaderboardService.addScore(ipId, modeId, scoreMid, 42_000_000L);

        List<LeaderboardEntryDto> top = realtimeLeaderboardService.getTopScores(ipId, modeId, 10);
        assertThat(top).hasSize(3);
        assertThat(top.get(0).scoreEntryId()).isEqualTo(scoreHigh);
        assertThat(top.get(0).rank()).isEqualTo(1L);
        assertThat(top.get(1).scoreEntryId()).isEqualTo(scoreMid);
        assertThat(top.get(1).rank()).isEqualTo(2L);
        assertThat(top.get(2).scoreEntryId()).isEqualTo(scoreLow);
        assertThat(top.get(2).rank()).isEqualTo(3L);

        assertThat(realtimeLeaderboardService.getRank(ipId, modeId, scoreHigh)).contains(1L);
        assertThat(realtimeLeaderboardService.getRank(ipId, modeId, scoreMid)).contains(2L);
        assertThat(realtimeLeaderboardService.getRank(ipId, modeId, scoreLow)).contains(3L);

        // DTO Cache test
        List<LeaderboardRowDto> dtoList = List.of(
                new LeaderboardRowDto(1L, scoreHigh, "MAX", "Max Pinball", 95_000_000L, Instant.now(), "Godzilla", "Classic")
        );
        realtimeLeaderboardService.setCachedLeaderboard(ipId, modeId, 10, dtoList, Duration.ofSeconds(60));
        assertThat(realtimeLeaderboardService.getCachedLeaderboard(ipId, modeId, 10)).isPresent();

        realtimeLeaderboardService.invalidateCachedLeaderboard(ipId, modeId);
        assertThat(realtimeLeaderboardService.getCachedLeaderboard(ipId, modeId, 10)).isEmpty();
    }

    @Test
    void shouldManageMachineStatusCacheAndLiveTelemetryInRedis() {
        UUID machineId = UUID.randomUUID();
        machineStatusCacheService.setStatus(machineId, "ACTIVE", Duration.ofMinutes(5));
        assertThat(machineStatusCacheService.getStatus(machineId)).contains("ACTIVE");

        machineStatusCacheService.invalidateStatus(machineId);
        assertThat(machineStatusCacheService.getStatus(machineId)).isEmpty();

        UUID sessionId = UUID.randomUUID();
        long afterFirstHit = liveTelemetryService.ingestDelta(sessionId, 1, 250_000L, "BUMPER_COMBO");
        long afterSecondHit = liveTelemetryService.ingestDelta(sessionId, 1, 750_000L, "JACKPOT");
        assertThat(afterFirstHit).isEqualTo(250_000L);
        assertThat(afterSecondHit).isEqualTo(1_000_000L);
    }

    @Test
    void shouldUseRedisSortedSetForMatchmakingQueue() throws InterruptedException {
        String machineId = "machine-" + UUID.randomUUID();
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        matchmakingQueueService.enqueue(machineId, player1);
        Thread.sleep(5);
        matchmakingQueueService.enqueue(machineId, player2);

        assertThat(matchmakingQueueService.dequeueNext(machineId)).contains(player1);
        assertThat(matchmakingQueueService.dequeueNext(machineId)).contains(player2);
        assertThat(matchmakingQueueService.dequeueNext(machineId)).isEmpty();
    }
}

package com.worldpinball.leaderboard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldpinball.leaderboard.api.filters.idempotency.HybridIdempotencyStore;
import com.worldpinball.leaderboard.api.filters.idempotency.IdempotentResponse;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.dtos.LeaderboardEntryDto;
import com.worldpinball.leaderboard.application.services.OutboxNotificationChannel;
import com.worldpinball.leaderboard.application.services.RealtimeLeaderboardService;
import com.worldpinball.leaderboard.domain.entities.OutboxMessage;
import com.worldpinball.leaderboard.domain.events.ScoreSubmittedDomainEvent;
import com.worldpinball.leaderboard.infrastructure.OutboxMessageRepository;
import com.worldpinball.leaderboard.infrastructure.kafka.LeaderboardScoreEventProcessor;
import com.worldpinball.leaderboard.infrastructure.outbox.OutboxCleaner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.outbox.worker.enabled=false",
        "app.outbox.cleanup.enabled=false"
})
class OutboxAndIdempotencyIntegrationTest {

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private OutboxNotificationChannel outboxNotificationChannel;

    @Autowired
    private OutboxCleaner outboxCleaner;

    @Autowired
    private LeaderboardScoreEventProcessor scoreEventProcessor;

    @Autowired
    private RealtimeLeaderboardService realtimeLeaderboardService;

    @Autowired
    private HybridIdempotencyStore hybridIdempotencyStore;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void outboxMessageLifecycle_andCleanerRemovesExpiredProcessedMessages() throws Exception {
        UUID msgId = UUID.randomUUID();
        Instant tenDaysAgo = Instant.now().minus(Duration.ofDays(10));

        OutboxMessage message = new OutboxMessage(
                msgId,
                "ScoreSubmittedDomainEvent",
                "{\"score\":999999}",
                tenDaysAgo
        );
        outboxMessageRepository.save(message);

        outboxNotificationChannel.tryNotify();
        assertThat(outboxNotificationChannel.waitForSignalOrTimeout(Duration.ofMillis(100))).isTrue();
        outboxNotificationChannel.drain();

        List<OutboxMessage> unprocessed = outboxMessageRepository.findUnprocessedBatchForUpdate(20);
        assertThat(unprocessed).extracting(OutboxMessage::getId).contains(msgId);

        message.markAsProcessed(tenDaysAgo);
        outboxMessageRepository.save(message);

        int deletedCount = outboxCleaner.cleanupBatch(Duration.ofDays(7), 100);
        assertThat(deletedCount).isGreaterThanOrEqualTo(1);
        assertThat(outboxMessageRepository.findById(msgId)).isEmpty();
    }

    @Test
    void leaderboardScoreEventProcessor_addsScoreToRedisZSet() throws Exception {
        UUID ipId = UUID.randomUUID();
        UUID modeId = UUID.randomUUID();
        UUID scoreEntryId = UUID.randomUUID();

        ScoreSubmittedDomainEvent event = new ScoreSubmittedDomainEvent(
                scoreEntryId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                ipId,
                modeId,
                1,
                UUID.randomUUID().toString(),
                750_000_000L,
                Instant.now()
        );

        String json = objectMapper.writeValueAsString(event);
        scoreEventProcessor.processMessage(json);

        List<LeaderboardEntryDto> topEntries = realtimeLeaderboardService.getTopScores(ipId, modeId, 10);
        assertThat(topEntries).extracting(LeaderboardEntryDto::scoreEntryId).containsExactly(scoreEntryId);
    }

    @Test
    void hybridIdempotencyStore_persistsToMemoryAndRedis() {
        String key = "POST:/api/v1/admin/machines/provision:" + UUID.randomUUID();
        IdempotentResponse expected = new IdempotentResponse(
                200,
                "application/json",
                "{\"machineId\":\"11111111-1111-1111-1111-111111111111\"}"
        );

        hybridIdempotencyStore.set(key, expected, Duration.ofMinutes(5));

        Optional<IdempotentResponse> actual = hybridIdempotencyStore.get(key);
        assertThat(actual).isPresent();
        assertThat(actual.get()).isEqualTo(expected);

        String redisRaw = redisTemplate.opsForValue().get("idempotency:" + key);
        assertThat(redisRaw).isNotBlank();
    }
}

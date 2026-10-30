package com.worldpinball.leaderboard.infrastructure.redis;

import com.worldpinball.leaderboard.application.services.MatchmakingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Mirror of C#'s RedisMatchmakingQueueService.
 * Uses a Redis Sorted Set (ZSET) scored by arrival timestamp (epoch ms) for FIFO matchmaking per machine.
 */
@Service
@Primary
@RequiredArgsConstructor
public class RedisMatchmakingQueueService implements MatchmakingQueueService {

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    private static String getKey(String machineId) {
        return "matchmaking:queue:" + machineId;
    }

    @Override
    public void enqueue(String machineId, UUID playerId) {
        double score = Instant.now(clock).toEpochMilli();
        redisTemplate.opsForZSet().add(getKey(machineId), playerId.toString(), score);
    }

    @Override
    public Optional<UUID> dequeueNext(String machineId) {
        ZSetOperations.TypedTuple<String> entry = redisTemplate.opsForZSet().popMin(getKey(machineId));
        if (entry == null || entry.getValue() == null || entry.getValue().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(entry.getValue()));
    }

    @Override
    public boolean remove(String machineId, UUID playerId) {
        Long removed = redisTemplate.opsForZSet().remove(getKey(machineId), playerId.toString());
        return removed != null && removed > 0;
    }
}

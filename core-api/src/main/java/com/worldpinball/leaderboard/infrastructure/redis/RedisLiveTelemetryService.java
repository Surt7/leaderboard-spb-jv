package com.worldpinball.leaderboard.infrastructure.redis;

import com.worldpinball.leaderboard.application.services.LiveTelemetryService;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Mirror of C#'s RedisLiveTelemetryService.
 * Atomically increments volatile ball telemetry score in Redis and broadcasts via LobbyNotificationService.
 */
@Service
@RequiredArgsConstructor
public class RedisLiveTelemetryService implements LiveTelemetryService {

    private static final Duration TELEMETRY_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final LobbyNotificationService lobbyNotifier;

    @Override
    public long ingestDelta(UUID sessionId, int slotNumber, long pointsDelta, String triggerEvent) {
        if (pointsDelta <= 0) {
            throw new IllegalArgumentException("Points delta must be positive.");
        }

        String key = "telemetry:session:" + sessionId + ":slot:" + slotNumber;

        // 1. Atomically increment volatile score in Redis (INCRBY)
        Long currentTotal = redisTemplate.opsForValue().increment(key, pointsDelta);
        long total = currentTotal != null ? currentTotal : pointsDelta;
        redisTemplate.expire(key, TELEMETRY_TTL);

        // 2. Broadcast live score to spectators
        lobbyNotifier.notifyScoreSubmitted(sessionId, slotNumber, total);

        return total;
    }
}

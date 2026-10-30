package com.worldpinball.leaderboard.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.dtos.LeaderboardEntryDto;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.dtos.LeaderboardRowDto;
import com.worldpinball.leaderboard.application.services.RealtimeLeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * High-performance in-memory leaderboard engine backed by Redis Sorted Sets (ZSET).
 * Mirror of C#'s RedisRealtimeLeaderboardService.
 * Partitions leaderboards dynamically by IP license and game mode (leaderboard:{ipId}:{modeId}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRealtimeLeaderboardService implements RealtimeLeaderboardService {

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static String getKey(UUID ipId, UUID modeId) {
        return "leaderboard:" + ipId + ":" + modeId;
    }

    private static String getDtoCacheKey(UUID ipId, UUID modeId, int limit) {
        return "leaderboard:dto:" + ipId + ":" + modeId + ":" + limit;
    }

    @Override
    public void addScore(UUID ipId, UUID modeId, UUID scoreEntryId, long score) {
        String key = getKey(ipId, modeId);
        redisTemplate.opsForZSet().add(key, scoreEntryId.toString(), (double) score);
    }

    @Override
    public List<LeaderboardEntryDto> getTopScores(UUID ipId, UUID modeId, int limit) {
        String key = getKey(ipId, modeId);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<LeaderboardEntryDto> result = new ArrayList<>(tuples.size());
        long rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String value = tuple.getValue();
            Double scoreDouble = tuple.getScore();
            if (value != null && scoreDouble != null && scoreDouble >= 0) {
                try {
                    UUID scoreEntryId = UUID.fromString(value);
                    result.add(new LeaderboardEntryDto(scoreEntryId, scoreDouble.longValue(), rank++));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return result;
    }

    @Override
    public Optional<Long> getRank(UUID ipId, UUID modeId, UUID scoreEntryId) {
        String key = getKey(ipId, modeId);
        Long rankIndex = redisTemplate.opsForZSet().reverseRank(key, scoreEntryId.toString());
        if (rankIndex == null) {
            return Optional.empty();
        }
        return Optional.of(rankIndex + 1);
    }

    @Override
    public Optional<List<LeaderboardRowDto>> getCachedLeaderboard(UUID ipId, UUID modeId, int limit) {
        String key = getDtoCacheKey(ipId, modeId, limit);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            List<LeaderboardRowDto> rows = objectMapper.readValue(json, new TypeReference<>() {});
            return Optional.of(rows);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize cached leaderboard for key {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void setCachedLeaderboard(UUID ipId, UUID modeId, int limit, List<LeaderboardRowDto> rows, Duration ttl) {
        String key = getDtoCacheKey(ipId, modeId, limit);
        Duration expiry = ttl != null ? ttl : DEFAULT_CACHE_TTL;
        try {
            String json = objectMapper.writeValueAsString(rows);
            redisTemplate.opsForValue().set(key, json, expiry);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize leaderboard cache for key {}: {}", key, ex.getMessage());
        }
    }

    @Override
    public void invalidateCachedLeaderboard(UUID ipId, UUID modeId) {
        String pattern = "leaderboard:dto:" + ipId + ":" + modeId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}

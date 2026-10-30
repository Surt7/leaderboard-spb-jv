package com.worldpinball.leaderboard.api.filters.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class HybridIdempotencyStore implements IdempotencyStore {

    private final InMemoryIdempotencyStore localStore;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<IdempotentResponse> get(String key) {
        Optional<IdempotentResponse> localCached = localStore.get(key);
        if (localCached.isPresent()) {
            return localCached;
        }

        try {
            String redisVal = redisTemplate.opsForValue().get("idempotency:" + key);
            if (redisVal != null && !redisVal.isBlank()) {
                IdempotentResponse response = objectMapper.readValue(redisVal, IdempotentResponse.class);
                if (response != null) {
                    localStore.set(key, response, Duration.ofMinutes(30));
                    return Optional.of(response);
                }
            }
        } catch (Exception ex) {
            log.warn("Redis lookup failed for key {}, falling back to local memory", key, ex);
        }

        return Optional.empty();
    }

    @Override
    public void set(String key, IdempotentResponse response, Duration duration) {
        localStore.set(key, response, duration);

        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set("idempotency:" + key, json, duration);
        } catch (Exception ex) {
            log.warn("Redis write failed for key {}, continuing with local memory only", key, ex);
        }
    }
}

package com.worldpinball.leaderboard.api.filters.idempotency;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private record CacheEntry(IdempotentResponse response, Instant expiresAt) {}

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<IdempotentResponse> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            if (!Instant.now().isAfter(entry.expiresAt())) {
                return Optional.of(entry.response());
            }
            cache.remove(key);
        }
        return Optional.empty();
    }

    @Override
    public void set(String key, IdempotentResponse response, Duration duration) {
        Instant expiresAt = Instant.now().plus(duration);
        cache.put(key, new CacheEntry(response, expiresAt));
    }
}

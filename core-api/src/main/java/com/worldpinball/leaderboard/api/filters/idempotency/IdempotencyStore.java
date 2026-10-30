package com.worldpinball.leaderboard.api.filters.idempotency;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStore {
    Optional<IdempotentResponse> get(String key);
    void set(String key, IdempotentResponse response, Duration duration);
}

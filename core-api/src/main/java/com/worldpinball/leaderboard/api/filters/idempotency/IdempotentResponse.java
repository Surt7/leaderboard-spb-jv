package com.worldpinball.leaderboard.api.filters.idempotency;

public record IdempotentResponse(
        int statusCode,
        String contentType,
        String body
) {
}

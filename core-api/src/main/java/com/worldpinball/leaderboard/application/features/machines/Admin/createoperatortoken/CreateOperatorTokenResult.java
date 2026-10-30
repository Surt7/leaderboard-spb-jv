package com.worldpinball.leaderboard.application.features.machines.Admin.createoperatortoken;

import java.time.Instant;
import java.util.UUID;

public sealed interface CreateOperatorTokenResult {

    record Success(
            UUID tokenId,
            String name,
            String rawToken,
            String prefix,
            String scopes,
            Instant expiresAtUtc
    ) implements CreateOperatorTokenResult {}

    record InvalidScopes(String reason) implements CreateOperatorTokenResult {}
}

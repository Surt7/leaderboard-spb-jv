package com.worldpinball.leaderboard.application.features.machines.Admin.createoperatortoken;

import java.time.Instant;
import java.util.UUID;

public record CreateOperatorTokenResponse(
        UUID tokenId,
        String name,
        String rawToken,
        String prefix,
        String scopes,
        Instant expiresAtUtc
) {}

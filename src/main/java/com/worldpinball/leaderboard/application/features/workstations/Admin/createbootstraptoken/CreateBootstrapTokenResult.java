package com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken;

import com.worldpinball.leaderboard.domain.enums.WorkstationType;

import java.time.Instant;
import java.util.UUID;

public sealed interface CreateBootstrapTokenResult {
    record Success(
            UUID tokenId,
            String rawBootstrapToken,
            Instant expiresAtUtc,
            WorkstationType workstationType
    ) implements CreateBootstrapTokenResult {}

    record Failure(String reason) implements CreateBootstrapTokenResult {}
}

package com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.dtos;

import com.worldpinball.leaderboard.domain.enums.WorkstationType;

import java.time.Instant;
import java.util.UUID;

public record CreateBootstrapTokenResponse(
        UUID tokenId,
        String rawBootstrapToken,
        Instant expiresAtUtc,
        WorkstationType workstationType
) {}

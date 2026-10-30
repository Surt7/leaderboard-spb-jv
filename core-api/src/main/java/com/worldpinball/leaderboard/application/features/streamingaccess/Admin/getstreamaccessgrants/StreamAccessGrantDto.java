package com.worldpinball.leaderboard.application.features.streamingaccess.Admin.getstreamaccessgrants;

import com.worldpinball.leaderboard.domain.enums.GrantOrigin;
import com.worldpinball.leaderboard.domain.enums.GrantStatus;
import com.worldpinball.leaderboard.domain.enums.PrincipalType;

import java.time.Instant;
import java.util.UUID;

public record StreamAccessGrantDto(
        UUID grantId,
        PrincipalType principalType,
        UUID principalId,
        String displayName,
        GrantOrigin origin,
        GrantStatus status,
        Instant createdAtUtc,
        Instant expiresAtUtc,
        Instant revokedAtUtc,
        String revocationReason,
        UUID grantedByUserId
) {}

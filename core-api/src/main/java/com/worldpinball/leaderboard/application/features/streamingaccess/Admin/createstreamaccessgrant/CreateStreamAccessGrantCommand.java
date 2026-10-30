package com.worldpinball.leaderboard.application.features.streamingaccess.Admin.createstreamaccessgrant;

import com.worldpinball.leaderboard.domain.enums.GrantOrigin;
import com.worldpinball.leaderboard.domain.enums.PrincipalType;

import java.time.Instant;
import java.util.UUID;

public record CreateStreamAccessGrantCommand(
        PrincipalType principalType,
        UUID principalId,
        String displayName,
        GrantOrigin origin,
        Instant expiresAtUtc,
        UUID grantedByUserId
) {}

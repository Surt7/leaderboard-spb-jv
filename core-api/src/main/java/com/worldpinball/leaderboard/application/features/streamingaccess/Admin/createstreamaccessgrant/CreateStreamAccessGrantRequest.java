package com.worldpinball.leaderboard.application.features.streamingaccess.Admin.createstreamaccessgrant;

import com.worldpinball.leaderboard.domain.enums.GrantOrigin;
import com.worldpinball.leaderboard.domain.enums.PrincipalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateStreamAccessGrantRequest(
        @NotNull(message = "PrincipalType is required.")
        PrincipalType principalType,

        @NotNull(message = "PrincipalId is required.")
        UUID principalId,

        @NotBlank(message = "DisplayName is required.")
        String displayName,

        @NotNull(message = "Origin is required.")
        GrantOrigin origin,

        Instant expiresAtUtc
) {}

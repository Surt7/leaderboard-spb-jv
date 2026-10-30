package com.worldpinball.leaderboard.application.features.machines.Admin.createoperatortoken;

import jakarta.validation.constraints.NotBlank;

public record CreateOperatorTokenRequest(
        @NotBlank(message = "Token name is required.")
        String name,
        String scopes,
        Integer validityDays
) {}

package com.worldpinball.leaderboard.application.features.machines.Admin.createoperatortoken;

public record CreateOperatorTokenCommand(
        String name,
        String scopes,
        Integer validityDays,
        String issuedByUserId
) {}

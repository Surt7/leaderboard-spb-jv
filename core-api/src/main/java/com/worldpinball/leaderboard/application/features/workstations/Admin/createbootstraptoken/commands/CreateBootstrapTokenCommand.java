package com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.commands;

import com.worldpinball.leaderboard.domain.enums.WorkstationType;

public record CreateBootstrapTokenCommand(
        String issuedByUserId,
        WorkstationType workstationType,
        String notes,
        int validityHours
) {}

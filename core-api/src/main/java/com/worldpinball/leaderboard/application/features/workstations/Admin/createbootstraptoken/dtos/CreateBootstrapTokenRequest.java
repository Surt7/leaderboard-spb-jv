package com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.dtos;

import com.worldpinball.leaderboard.domain.enums.WorkstationType;

public record CreateBootstrapTokenRequest(
        WorkstationType workstationType,
        String notes,
        Integer validityHours
) {
    public WorkstationType resolvedWorkstationType() {
        return workstationType != null ? workstationType : WorkstationType.FieldTerminal;
    }

    public int resolvedValidityHours() {
        return validityHours != null ? validityHours : 2;
    }
}

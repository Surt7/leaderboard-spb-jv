package com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant;

import jakarta.validation.constraints.NotBlank;

public record RevokeStreamAccessGrantRequest(
        @NotBlank(message = "Reason is required.")
        String reason
) {
}

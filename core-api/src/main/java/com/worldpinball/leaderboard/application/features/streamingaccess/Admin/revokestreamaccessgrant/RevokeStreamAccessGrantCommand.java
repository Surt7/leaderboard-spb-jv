package com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant;

import java.util.UUID;

public record RevokeStreamAccessGrantCommand(
        UUID grantId,
        String reason
) {
}

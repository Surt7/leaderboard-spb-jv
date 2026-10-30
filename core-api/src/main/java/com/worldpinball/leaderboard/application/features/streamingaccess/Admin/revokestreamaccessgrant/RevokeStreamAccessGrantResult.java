package com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant;

import java.util.UUID;

public sealed interface RevokeStreamAccessGrantResult {
    record Success(UUID grantId) implements RevokeStreamAccessGrantResult {}
    record NotFound() implements RevokeStreamAccessGrantResult {}
    record AlreadyRevoked() implements RevokeStreamAccessGrantResult {}
    record InvalidReason(String error) implements RevokeStreamAccessGrantResult {}
}

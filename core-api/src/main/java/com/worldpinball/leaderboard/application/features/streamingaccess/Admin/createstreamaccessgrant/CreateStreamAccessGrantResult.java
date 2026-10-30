package com.worldpinball.leaderboard.application.features.streamingaccess.Admin.createstreamaccessgrant;

import java.util.UUID;

public sealed interface CreateStreamAccessGrantResult {
    record Success(UUID grantId) implements CreateStreamAccessGrantResult {}
    record AlreadyExists(UUID grantId) implements CreateStreamAccessGrantResult {}
    record InvalidData(String error) implements CreateStreamAccessGrantResult {}
}

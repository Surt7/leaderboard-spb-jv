package com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation;

import java.time.Instant;

public sealed interface EnrollWorkstationResult permits
        EnrollWorkstationResult.Success,
        EnrollWorkstationResult.InvalidToken,
        EnrollWorkstationResult.InvalidPublicKey {

    record Success(
            String certPem,
            String serialNumber,
            String thumbprintSha256,
            Instant notAfterUtc
    ) implements EnrollWorkstationResult {
    }

    record InvalidToken(String reason) implements EnrollWorkstationResult {
    }

    record InvalidPublicKey(String reason) implements EnrollWorkstationResult {
    }
}

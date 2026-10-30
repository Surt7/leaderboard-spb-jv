package com.worldpinball.leaderboard.application.features.machines.Admin.reissuetoken.dtos;

import java.time.Instant;
import java.util.UUID;

public record ReissueEnrollmentTokenResponse(
        UUID machineId,
        String enrollmentToken,
        Instant expiresAtUtc
) {}

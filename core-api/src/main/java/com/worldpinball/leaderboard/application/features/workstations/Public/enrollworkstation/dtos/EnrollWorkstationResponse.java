package com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.dtos;

import java.time.Instant;

public record EnrollWorkstationResponse(
        String certPem,
        String serialNumber,
        String thumbprintSha256,
        Instant notAfterUtc
) {
}

package com.worldpinball.leaderboard.application.security.ca;

import java.time.Instant;

public record IssuedWorkstationCertificate(
        String certPem,
        String serialNumber,
        String workstationName,
        String thumbprintSha256,
        String subjectDn,
        String issuerDn,
        Instant notBeforeUtc,
        Instant notAfterUtc
) {}

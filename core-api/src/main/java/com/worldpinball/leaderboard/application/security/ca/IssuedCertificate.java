package com.worldpinball.leaderboard.application.security.ca;

import java.time.Instant;
import java.util.UUID;

public record IssuedCertificate(
        String certPem,
        String serialNumber,
        UUID machineId,
        String thumbprintSha256,
        String subjectDn,
        String issuerDn,
        Instant notBeforeUtc,
        Instant notAfterUtc
) {
    public IssuedCertificate(String certPem, String serialNumber) {
        this(certPem, serialNumber, null, "", "", "", Instant.EPOCH, Instant.EPOCH);
    }
}

package com.worldpinball.leaderboard.application.security.ca;

import java.time.Instant;

public record IssuedSoftwareCertificate(
        String certPem,
        String serialNumber,
        String publisherName,
        String thumbprintSha256,
        String subjectDn,
        String issuerDn,
        Instant notBeforeUtc,
        Instant notAfterUtc
) {}

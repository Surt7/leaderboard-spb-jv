package com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.dtos;

import java.time.Instant;

public record IssueSoftwareCertificateResponse(
        String certPem,
        String serialNumber,
        String thumbprintSha256,
        Instant notAfterUtc
) {}

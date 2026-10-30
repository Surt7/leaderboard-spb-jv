package com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate;

import java.time.Instant;

public sealed interface IssueSoftwareCertificateResult {
    record Success(
            String certPem,
            String serialNumber,
            String thumbprintSha256,
            Instant notAfterUtc
    ) implements IssueSoftwareCertificateResult {}

    record InvalidPublicKey(String reason) implements IssueSoftwareCertificateResult {}
}

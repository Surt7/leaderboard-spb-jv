package com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate;

import com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.commands.IssueSoftwareCertificateCommand;
import com.worldpinball.leaderboard.application.security.ca.CertificateAuthorityService;
import com.worldpinball.leaderboard.application.security.ca.IssuedSoftwareCertificate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueSoftwareCertificateHandlerImpl implements IssueSoftwareCertificateHandler {

    private final CertificateAuthorityService caService;

    @Override
    public IssueSoftwareCertificateResult handle(IssueSoftwareCertificateCommand command) {
        if (command.publisherName() == null || command.publisherName().isBlank()) {
            return new IssueSoftwareCertificateResult.InvalidPublicKey("Publisher name is required.");
        }
        if (command.publicKeyPem() == null || command.publicKeyPem().isBlank()) {
            return new IssueSoftwareCertificateResult.InvalidPublicKey("Public key PEM is required.");
        }

        try {
            IssuedSoftwareCertificate issued = caService.issueSoftwareSigningCertificate(
                    command.publisherName().trim(),
                    command.publicKeyPem()
            );
            log.info("Issued code signing certificate for publisher {} (Serial: {})",
                    command.publisherName(), issued.serialNumber());
            return new IssueSoftwareCertificateResult.Success(
                    issued.certPem(),
                    issued.serialNumber(),
                    issued.thumbprintSha256(),
                    issued.notAfterUtc()
            );
        } catch (Exception ex) {
            log.warn("Failed to issue software certificate for publisher {}: invalid key format",
                    command.publisherName(), ex);
            return new IssueSoftwareCertificateResult.InvalidPublicKey(ex.getMessage());
        }
    }
}

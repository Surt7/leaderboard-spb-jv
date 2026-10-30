package com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation;

import com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.commands.EnrollWorkstationCommand;
import com.worldpinball.leaderboard.application.security.ca.CertificateAuthorityService;
import com.worldpinball.leaderboard.application.security.ca.IssuedWorkstationCertificate;
import com.worldpinball.leaderboard.application.security.secrets.SecretService;
import com.worldpinball.leaderboard.domain.entities.EnrollmentToken;
import com.worldpinball.leaderboard.domain.enums.EnrollmentTokenType;
import com.worldpinball.leaderboard.domain.enums.WorkstationType;
import com.worldpinball.leaderboard.infrastructure.EnrollmentTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class EnrollWorkstationHandlerImpl implements EnrollWorkstationHandler {

    private static final Logger log = LoggerFactory.getLogger(EnrollWorkstationHandlerImpl.class);

    private final EnrollmentTokenRepository enrollmentTokenRepository;
    private final CertificateAuthorityService caService;
    private final SecretService secretService;

    public EnrollWorkstationHandlerImpl(
            EnrollmentTokenRepository enrollmentTokenRepository,
            CertificateAuthorityService caService,
            SecretService secretService
    ) {
        this.enrollmentTokenRepository = enrollmentTokenRepository;
        this.caService = caService;
        this.secretService = secretService;
    }

    @Override
    @Transactional
    public EnrollWorkstationResult handle(EnrollWorkstationCommand command) {
        if (command.plainBootstrapToken() == null || command.plainBootstrapToken().isBlank()) {
            return new EnrollWorkstationResult.InvalidToken("Bootstrap token cannot be empty.");
        }

        if (command.workstationName() == null || command.workstationName().isBlank()) {
            return new EnrollWorkstationResult.InvalidToken("Workstation name cannot be empty.");
        }

        if (command.publicKeyPem() == null || command.publicKeyPem().isBlank()) {
            return new EnrollWorkstationResult.InvalidPublicKey("Public key PEM cannot be empty.");
        }

        Optional<String> tokenHashOpt = secretService.tryHash(command.plainBootstrapToken());
        if (tokenHashOpt.isEmpty()) {
            log.warn("Workstation enrollment rejected for {}: invalid token hash format.", command.workstationName());
            return new EnrollWorkstationResult.InvalidToken("Invalid token format.");
        }

        Optional<EnrollmentToken> tokenOpt = enrollmentTokenRepository.findByTokenHash(tokenHashOpt.get());
        if (tokenOpt.isEmpty()) {
            log.warn("Workstation enrollment rejected for {}: token not found.", command.workstationName());
            return new EnrollWorkstationResult.InvalidToken("Token not found.");
        }

        EnrollmentToken token = tokenOpt.get();
        if (token.getTokenType() != EnrollmentTokenType.AdminBootstrap
                && token.getTokenType() != EnrollmentTokenType.TerminalBootstrap) {
            log.warn("Workstation enrollment rejected for {}: token type mismatch ({}).",
                    command.workstationName(), token.getTokenType());
            return new EnrollWorkstationResult.InvalidToken("Token is not valid for workstation enrollment.");
        }

        WorkstationType workstationType = token.getTokenType() == EnrollmentTokenType.AdminBootstrap
                ? WorkstationType.AdminWorkstation
                : WorkstationType.FieldTerminal;

        Instant now = Instant.now();

        try {
            token.consume(now);
        } catch (IllegalStateException ex) {
            log.warn("Workstation enrollment rejected for {}: {}", command.workstationName(), ex.getMessage());
            return new EnrollWorkstationResult.InvalidToken(ex.getMessage());
        }

        enrollmentTokenRepository.save(token);

        try {
            IssuedWorkstationCertificate issuedCert = caService.issueWorkstationCertificate(
                    command.workstationName(),
                    command.publicKeyPem(),
                    workstationType
            );

            log.info("Workstation {} successfully enrolled with CertSerial {}",
                    command.workstationName(), issuedCert.serialNumber());

            return new EnrollWorkstationResult.Success(
                    issuedCert.certPem(),
                    issuedCert.serialNumber(),
                    issuedCert.thumbprintSha256(),
                    issuedCert.notAfterUtc()
            );
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to issue certificate for workstation {}: invalid key", command.workstationName(), ex);
            return new EnrollWorkstationResult.InvalidPublicKey(ex.getMessage());
        }
    }
}

package com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken;

import com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.commands.CreateBootstrapTokenCommand;
import com.worldpinball.leaderboard.application.security.secrets.SecretGenerationResult;
import com.worldpinball.leaderboard.application.security.secrets.SecretService;
import com.worldpinball.leaderboard.domain.entities.EnrollmentToken;
import com.worldpinball.leaderboard.domain.enums.EnrollmentTokenType;
import com.worldpinball.leaderboard.domain.enums.WorkstationType;
import com.worldpinball.leaderboard.infrastructure.EnrollmentTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateBootstrapTokenHandlerImpl implements CreateBootstrapTokenHandler {

    private final EnrollmentTokenRepository enrollmentTokenRepository;
    private final SecretService secretService;

    @Override
    @Transactional
    public CreateBootstrapTokenResult handle(CreateBootstrapTokenCommand command) {
        if (command.issuedByUserId() == null || command.issuedByUserId().isBlank()) {
            return new CreateBootstrapTokenResult.Failure("IssuedByUserId is required.");
        }
        if (command.validityHours() <= 0) {
            return new CreateBootstrapTokenResult.Failure("ValidityHours must be greater than zero.");
        }

        WorkstationType wsType = command.workstationType() != null ? command.workstationType() : WorkstationType.FieldTerminal;
        EnrollmentTokenType tokenType = switch (wsType) {
            case AdminWorkstation -> EnrollmentTokenType.AdminBootstrap;
            case FieldTerminal -> EnrollmentTokenType.TerminalBootstrap;
        };

        Instant nowUtc = Instant.now();
        Instant expiresAtUtc = nowUtc.plus(command.validityHours(), ChronoUnit.HOURS);

        SecretGenerationResult secret = secretService.generate();

        EnrollmentToken token = new EnrollmentToken(
                secret.hashHex(),
                tokenType,
                command.issuedByUserId(),
                nowUtc,
                expiresAtUtc,
                null,
                null,
                1,
                command.notes(),
                command.validityHours()
        );

        enrollmentTokenRepository.save(token);

        log.info("Workstation bootstrap OTP {} created by {}. Valid for {} hours (expires {})",
                token.getTokenId(), command.issuedByUserId(), command.validityHours(), expiresAtUtc);

        return new CreateBootstrapTokenResult.Success(
                token.getTokenId(),
                secret.plainBase64(),
                expiresAtUtc,
                wsType
        );
    }
}

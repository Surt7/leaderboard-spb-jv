package com.worldpinball.leaderboard.application.features.machines.Admin.createoperatortoken;

import com.worldpinball.leaderboard.domain.entities.OperatorToken;
import com.worldpinball.leaderboard.infrastructure.OperatorTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
public class CreateOperatorTokenHandlerImpl implements CreateOperatorTokenHandler {

    private static final Logger log = LoggerFactory.getLogger(CreateOperatorTokenHandlerImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OperatorTokenRepository repository;
    private final Clock clock;

    public CreateOperatorTokenHandlerImpl(OperatorTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreateOperatorTokenResult handle(CreateOperatorTokenCommand command) {
        Instant nowUtc = clock.instant();

        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = "wpt_live_" + HexFormat.of().formatHex(randomBytes).toLowerCase();

        Instant expiresAtUtc = (command.validityDays() != null && command.validityDays() > 0)
                ? nowUtc.plus(command.validityDays(), ChronoUnit.DAYS)
                : null;

        OperatorToken token = new OperatorToken(
                command.name(),
                rawToken,
                command.scopes(),
                command.issuedByUserId(),
                expiresAtUtc,
                nowUtc
        );

        repository.save(token);

        log.info("[2060:OperatorTokenCreated] Operator token created: {} ({}) with scopes: {} by user {}",
                token.getName(),
                token.getPrefix(),
                token.getScopes(),
                command.issuedByUserId());

        return new CreateOperatorTokenResult.Success(
                token.getTokenId(),
                token.getName(),
                rawToken,
                token.getPrefix(),
                token.getScopes(),
                token.getExpiresAtUtc()
        );
    }
}

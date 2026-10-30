package com.worldpinball.leaderboard.application.features.streamingaccess.Admin.createstreamaccessgrant;

import com.worldpinball.leaderboard.domain.entities.StreamAccessGrant;
import com.worldpinball.leaderboard.domain.enums.GrantStatus;
import com.worldpinball.leaderboard.infrastructure.StreamAccessGrantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class CreateStreamAccessGrantHandlerImpl implements CreateStreamAccessGrantHandler {

    private static final Logger log = LoggerFactory.getLogger(CreateStreamAccessGrantHandlerImpl.class);

    private final StreamAccessGrantRepository repository;
    private final Clock clock;

    public CreateStreamAccessGrantHandlerImpl(StreamAccessGrantRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreateStreamAccessGrantResult handle(CreateStreamAccessGrantCommand command) {
        Instant nowUtc = clock.instant();

        Optional<StreamAccessGrant> existing = repository.findByPrincipalTypeAndPrincipalIdAndStatus(
                command.principalType(),
                command.principalId(),
                GrantStatus.ACTIVE
        );

        if (existing.isPresent()) {
            return new CreateStreamAccessGrantResult.AlreadyExists(existing.get().getGrantId());
        }

        try {
            StreamAccessGrant grant = new StreamAccessGrant(
                    command.principalType(),
                    command.principalId(),
                    command.displayName(),
                    command.origin(),
                    nowUtc,
                    command.expiresAtUtc(),
                    command.grantedByUserId()
            );

            repository.save(grant);

            log.info("Stream access grant created: {} for principal {} ({})",
                    grant.getGrantId(), command.principalId(), command.displayName());

            return new CreateStreamAccessGrantResult.Success(grant.getGrantId());
        } catch (IllegalArgumentException e) {
            return new CreateStreamAccessGrantResult.InvalidData(e.getMessage());
        }
    }
}

package com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant;

import com.worldpinball.leaderboard.domain.entities.StreamAccessGrant;
import com.worldpinball.leaderboard.domain.enums.GrantStatus;
import com.worldpinball.leaderboard.infrastructure.StreamAccessGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RevokeStreamAccessGrantHandlerImpl implements RevokeStreamAccessGrantHandler {

    private final StreamAccessGrantRepository grantRepository;

    @Override
    @Transactional
    public RevokeStreamAccessGrantResult handle(RevokeStreamAccessGrantCommand command) {
        if (command.reason() == null || command.reason().isBlank()) {
            return new RevokeStreamAccessGrantResult.InvalidReason("Revocation reason cannot be empty.");
        }

        Optional<StreamAccessGrant> grantOpt = grantRepository.findById(command.grantId());
        if (grantOpt.isEmpty()) {
            return new RevokeStreamAccessGrantResult.NotFound();
        }

        StreamAccessGrant grant = grantOpt.get();
        if (grant.getStatus() == GrantStatus.REVOKED) {
            return new RevokeStreamAccessGrantResult.AlreadyRevoked();
        }

        grant.revoke(command.reason(), Instant.now());
        grantRepository.save(grant);

        return new RevokeStreamAccessGrantResult.Success(grant.getGrantId());
    }
}

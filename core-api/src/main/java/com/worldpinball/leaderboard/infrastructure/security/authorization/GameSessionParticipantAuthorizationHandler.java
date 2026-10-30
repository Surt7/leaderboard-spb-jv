package com.worldpinball.leaderboard.infrastructure.security.authorization;

import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.infrastructure.security.authentication.ClaimsPrincipal;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Verifies that the caller's {@code sub} / NameIdentifier matches either the hosting machine
 * of the {@link GameSession} or one of its registered session players.
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.Authorization.GameSessionParticipantAuthorizationHandler}.
 */
@Component
public class GameSessionParticipantAuthorizationHandler {

    public boolean isAuthorized(ClaimsPrincipal user, GameSession resource) {
        if (user == null || resource == null) {
            return false;
        }

        Optional<String> rawUserId = user.findFirst(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER);
        if (rawUserId.isEmpty()) {
            return false;
        }

        UUID callerId;
        try {
            callerId = UUID.fromString(rawUserId.get());
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (callerId.equals(resource.getMachineId())) {
            return true;
        }

        return resource.getPlayers().stream()
                .anyMatch(p -> callerId.equals(p.getUserId()));
    }
}

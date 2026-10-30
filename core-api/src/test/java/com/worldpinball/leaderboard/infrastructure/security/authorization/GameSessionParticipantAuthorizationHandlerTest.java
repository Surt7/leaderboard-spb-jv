package com.worldpinball.leaderboard.infrastructure.security.authorization;

import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.infrastructure.security.authentication.ClaimsPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameSessionParticipantAuthorizationHandler Tests")
class GameSessionParticipantAuthorizationHandlerTest {

    @Test
    @DisplayName("Succeeds when machineId matches the session's machineId")
    void handle_whenMachineMatchesSession_succeeds() {
        UUID machineId = UUID.randomUUID();
        GameSession session = new GameSession(
                machineId, UUID.randomUUID(), UUID.randomUUID(), "1.0", "nonce", Instant.now(), null
        );

        GameSessionParticipantAuthorizationHandler sut = new GameSessionParticipantAuthorizationHandler();

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "Machine",
                List.of(new ClaimsPrincipal.Claim(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER, machineId.toString())),
                List.of("Machine")
        );

        boolean allowed = sut.isAuthorized(principal, session);
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Succeeds when userId is a registered participant in the session")
    void handle_whenPlayerInSession_succeeds() {
        UUID userId = UUID.randomUUID();
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0", "nonce", Instant.now(), null
        );
        session.addPlayer(1, userId);

        GameSessionParticipantAuthorizationHandler sut = new GameSessionParticipantAuthorizationHandler();

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "User",
                List.of(new ClaimsPrincipal.Claim(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER, userId.toString())),
                List.of("Player")
        );

        boolean allowed = sut.isAuthorized(principal, session);
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Fails when principal is neither the machine nor a registered player")
    void handle_whenUnrelatedUser_fails() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0", "nonce", Instant.now(), null
        );

        GameSessionParticipantAuthorizationHandler sut = new GameSessionParticipantAuthorizationHandler();

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "User",
                List.of(new ClaimsPrincipal.Claim(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER, UUID.randomUUID().toString())),
                List.of("Player")
        );

        boolean allowed = sut.isAuthorized(principal, session);
        assertThat(allowed).isFalse();
    }
}

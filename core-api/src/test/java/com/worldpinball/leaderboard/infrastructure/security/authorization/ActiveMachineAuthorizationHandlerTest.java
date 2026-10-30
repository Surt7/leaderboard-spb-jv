package com.worldpinball.leaderboard.infrastructure.security.authorization;

import com.worldpinball.leaderboard.infrastructure.security.authentication.ClaimsPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ActiveMachineAuthorizationHandler Tests")
class ActiveMachineAuthorizationHandlerTest {

    @Test
    @DisplayName("Succeeds when principal is Machine and status is ACTIVE")
    void handle_whenActiveMachine_succeeds() {
        ActiveMachineAuthorizationHandler sut = new ActiveMachineAuthorizationHandler();

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "Machine",
                List.of(
                        new ClaimsPrincipal.Claim(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER, UUID.randomUUID().toString()),
                        new ClaimsPrincipal.Claim("machine_status", "ACTIVE")
                ),
                List.of("Machine")
        );

        boolean allowed = sut.isAuthorized(principal);
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Fails when principal is Machine but status is not ACTIVE (e.g. SUSPENDED)")
    void handle_whenSuspendedMachine_fails() {
        ActiveMachineAuthorizationHandler sut = new ActiveMachineAuthorizationHandler();

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "Machine",
                List.of(
                        new ClaimsPrincipal.Claim(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER, UUID.randomUUID().toString()),
                        new ClaimsPrincipal.Claim("machine_status", "SUSPENDED")
                ),
                List.of("Machine")
        );

        boolean allowed = sut.isAuthorized(principal);
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Fails when principal is missing machine_status claim")
    void handle_whenMissingStatusClaim_fails() {
        ActiveMachineAuthorizationHandler sut = new ActiveMachineAuthorizationHandler();

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "Operator",
                List.of(),
                List.of("Operator")
        );

        boolean allowed = sut.isAuthorized(principal);
        assertThat(allowed).isFalse();
    }
}

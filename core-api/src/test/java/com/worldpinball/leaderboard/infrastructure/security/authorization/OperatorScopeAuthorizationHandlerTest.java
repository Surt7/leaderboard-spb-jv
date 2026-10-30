package com.worldpinball.leaderboard.infrastructure.security.authorization;

import com.worldpinball.leaderboard.infrastructure.security.authentication.ClaimsPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OperatorScopeAuthorizationHandler Tests")
class OperatorScopeAuthorizationHandlerTest {

    @Test
    @DisplayName("Succeeds when operator token possesses the required scope")
    void handle_whenScopeMatches_succeeds() {
        OperatorScopeAuthorizationHandler sut = new OperatorScopeAuthorizationHandler();

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "Operator",
                List.of(new ClaimsPrincipal.Claim("scope", "machines:read machines:write")),
                List.of("Operator")
        );

        boolean allowed = sut.isAuthorized(principal, "machines:read");
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Fails when operator token is missing the required scope")
    void handle_whenMissingScope_fails() {
        OperatorScopeAuthorizationHandler sut = new OperatorScopeAuthorizationHandler();

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "Operator",
                List.of(new ClaimsPrincipal.Claim("scope", "machines:read")),
                List.of("Operator")
        );

        boolean allowed = sut.isAuthorized(principal, "machines:write");
        assertThat(allowed).isFalse();
    }
}

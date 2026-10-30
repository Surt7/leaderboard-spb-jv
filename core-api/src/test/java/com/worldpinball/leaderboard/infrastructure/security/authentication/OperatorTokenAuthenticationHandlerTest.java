package com.worldpinball.leaderboard.infrastructure.security.authentication;

import com.worldpinball.leaderboard.domain.entities.OperatorToken;
import com.worldpinball.leaderboard.infrastructure.OperatorTokenRepository;
import com.worldpinball.leaderboard.infrastructure.security.authentication.OperatorTokenAuthenticationHandler.AuthenticateResult;
import com.worldpinball.leaderboard.infrastructure.security.authorization.OperatorScopeAuthorizationHandler;
import com.worldpinball.leaderboard.infrastructure.security.operatortokens.OperatorTokenValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorToken Authentication and Scope Authorization Tests")
class OperatorTokenAuthenticationHandlerTest {

    @Mock
    private OperatorTokenRepository operatorTokenRepository;

    @Test
    @DisplayName("OperatorTokenAuthenticationHandler authenticates valid token and OperatorScopeAuthorizationHandler checks scope")
    void operatorTokenAuthAndScopeHandler_validatesTokenAndScopes() {
        Instant fixedNow = Instant.parse("2026-10-19T08:00:00Z");
        Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        String rawToken = "op_live_1234567890abcdef12345678";

        OperatorToken token = new OperatorToken(
                "Factory Line #1",
                rawToken,
                "enroll, sessions",
                "admin-1",
                fixedNow.plus(Duration.ofDays(7)),
                fixedNow
        );

        when(operatorTokenRepository.findByTokenHash(OperatorToken.hashToken(rawToken)))
                .thenReturn(Optional.of(token));

        OperatorTokenValidator validator = new OperatorTokenValidator(operatorTokenRepository, fixedClock);
        OperatorTokenAuthenticationHandler authHandler = new OperatorTokenAuthenticationHandler(validator);
        OperatorScopeAuthorizationHandler scopeHandler = new OperatorScopeAuthorizationHandler();

        AuthenticateResult authResult = authHandler.authenticate(rawToken, null);
        assertThat(authResult).isInstanceOf(AuthenticateResult.Success.class);

        ClaimsPrincipal principal = ((AuthenticateResult.Success) authResult).principal();
        assertThat(scopeHandler.isAuthorized(principal, "enroll")).isTrue();
        assertThat(scopeHandler.isAuthorized(principal, "sessions")).isTrue();
        assertThat(scopeHandler.isAuthorized(principal, "unknown")).isFalse();

        // Second call hits in-memory L1 cache without querying DB again
        authHandler.authenticate(null, "Operator " + rawToken);
        verify(operatorTokenRepository, times(1)).findByTokenHash(any());
    }

    @Test
    @DisplayName("OperatorTokenAuthenticationHandler returns NoResult when header is null or blank")
    void authenticate_whenHeadersBlank_returnsNoResult() {
        OperatorTokenValidator validator = new OperatorTokenValidator(operatorTokenRepository);
        OperatorTokenAuthenticationHandler authHandler = new OperatorTokenAuthenticationHandler(validator);

        AuthenticateResult result = authHandler.authenticate(null, "");
        assertThat(result).isInstanceOf(AuthenticateResult.NoResult.class);
    }

    @Test
    @DisplayName("OperatorTokenAuthenticationHandler returns Fail on invalid token")
    void authenticate_whenInvalidToken_returnsFail() {
        when(operatorTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        OperatorTokenValidator validator = new OperatorTokenValidator(operatorTokenRepository);
        OperatorTokenAuthenticationHandler authHandler = new OperatorTokenAuthenticationHandler(validator);

        AuthenticateResult result = authHandler.authenticate("invalid-raw-token-1234", null);
        assertThat(result).isInstanceOf(AuthenticateResult.Fail.class);
    }
}

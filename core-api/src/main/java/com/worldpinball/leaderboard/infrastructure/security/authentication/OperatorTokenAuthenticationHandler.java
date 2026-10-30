package com.worldpinball.leaderboard.infrastructure.security.authentication;

import com.worldpinball.leaderboard.domain.entities.OperatorToken;
import com.worldpinball.leaderboard.infrastructure.security.authentication.ClaimsPrincipal.Claim;
import com.worldpinball.leaderboard.infrastructure.security.operatortokens.OperatorTokenValidator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Extracts and validates operator tokens from {@code X-Operator-Token} or {@code Authorization}
 * headers ({@code Bearer} / {@code Operator}) and constructs a {@link ClaimsPrincipal} with scopes.
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.Authentification.OperatorTokenAuthenticationHandler}.
 */
@Component
public class OperatorTokenAuthenticationHandler {

    public static final String SCHEME_NAME = "OperatorToken";
    public static final String HEADER_NAME = "X-Operator-Token";

    public sealed interface AuthenticateResult {
        record NoResult() implements AuthenticateResult {}
        record Success(ClaimsPrincipal principal) implements AuthenticateResult {}
        record Fail(String reason) implements AuthenticateResult {}
    }

    private final OperatorTokenValidator tokenValidator;

    public OperatorTokenAuthenticationHandler(OperatorTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    public AuthenticateResult authenticate(String operatorTokenHeader, String authorizationHeader) {
        String rawToken = null;

        if (operatorTokenHeader != null && !operatorTokenHeader.isBlank()) {
            rawToken = operatorTokenHeader;
        } else if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            if (authorizationHeader.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
                rawToken = authorizationHeader.substring("Bearer ".length()).trim();
            } else if (authorizationHeader.regionMatches(true, 0, "Operator ", 0, "Operator ".length())) {
                rawToken = authorizationHeader.substring("Operator ".length()).trim();
            }
        }

        if (rawToken == null || rawToken.isBlank()) {
            return new AuthenticateResult.NoResult();
        }

        Optional<OperatorToken> tokenOpt = tokenValidator.validate(rawToken);
        if (tokenOpt.isEmpty()) {
            return new AuthenticateResult.Fail("Invalid, expired, or revoked operator token.");
        }

        OperatorToken token = tokenOpt.get();
        List<Claim> claims = new ArrayList<>();
        claims.add(new Claim(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER, token.getTokenId().toString()));
        claims.add(new Claim(ClaimsPrincipal.CLAIM_NAME, token.getName()));
        claims.add(new Claim("operator_prefix", token.getPrefix()));

        for (String scope : token.getScopeList()) {
            claims.add(new Claim("scope", scope));
        }

        return new AuthenticateResult.Success(new ClaimsPrincipal(SCHEME_NAME, claims));
    }
}

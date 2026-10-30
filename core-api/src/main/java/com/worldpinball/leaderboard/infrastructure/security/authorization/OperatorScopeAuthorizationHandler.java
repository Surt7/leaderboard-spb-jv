package com.worldpinball.leaderboard.infrastructure.security.authorization;

import com.worldpinball.leaderboard.infrastructure.security.authentication.ClaimsPrincipal;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Verifies that an authenticated operator token possesses either wildcard {@code "*"}
 * or the required scope claim.
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.Authorization.OperatorScopeAuthorizationHandler}.
 */
@Component
public class OperatorScopeAuthorizationHandler {

    public boolean isAuthorized(ClaimsPrincipal user, String requiredScope) {
        if (user == null || requiredScope == null || requiredScope.isBlank()) {
            return false;
        }

        List<String> scopes = user.findAll("scope");
        return scopes.stream()
                .flatMap(s -> java.util.Arrays.stream(s.split("\\s+")))
                .anyMatch(s -> "*".equals(s) || s.equalsIgnoreCase(requiredScope));
    }
}

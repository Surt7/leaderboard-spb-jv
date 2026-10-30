package com.worldpinball.leaderboard.infrastructure.security.authentication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Lightweight claims principal modeling authenticated caller identity (User, Machine mTLS, or OperatorToken).
 * Mirrors .NET {@code System.Security.Claims.ClaimsPrincipal} for 1-to-1 authorization handler logic.
 */
public class ClaimsPrincipal {

    public static final String CLAIM_NAME_IDENTIFIER = "sub";
    public static final String CLAIM_NAME = "name";
    public static final String CLAIM_ROLE = "role";

    public record Claim(String type, String value) {}

    private final String authenticationScheme;
    private final List<Claim> claims = new ArrayList<>();

    public ClaimsPrincipal(String authenticationScheme, List<Claim> initialClaims) {
        this(authenticationScheme, initialClaims, Collections.emptyList());
    }

    public ClaimsPrincipal(String authenticationScheme, List<Claim> initialClaims, List<String> roles) {
        this.authenticationScheme = authenticationScheme != null ? authenticationScheme : "Default";
        if (initialClaims != null) {
            this.claims.addAll(initialClaims);
        }
        if (roles != null) {
            for (String role : roles) {
                this.claims.add(new Claim(CLAIM_ROLE, role));
            }
        }
    }

    public boolean hasScope(String scope) {
        if (scope == null) return false;
        return claims.stream()
                .filter(c -> c.type().equalsIgnoreCase("scope"))
                .anyMatch(c -> {
                    for (String part : c.value().split("[\\s,]+")) {
                        if (part.equalsIgnoreCase(scope) || "*".equals(part)) return true;
                    }
                    return false;
                });
    }

    public String getAuthenticationScheme() {
        return authenticationScheme;
    }

    public List<Claim> getClaims() {
        return Collections.unmodifiableList(claims);
    }

    public void addClaim(String type, String value) {
        if (type != null && value != null) {
            claims.add(new Claim(type, value));
        }
    }

    public Optional<String> findFirst(String type) {
        return claims.stream()
                .filter(c -> c.type().equalsIgnoreCase(type))
                .map(Claim::value)
                .findFirst();
    }

    public List<String> findAll(String type) {
        return claims.stream()
                .filter(c -> c.type().equalsIgnoreCase(type))
                .map(Claim::value)
                .toList();
    }

    public List<String> findAll(Predicate<Claim> predicate) {
        return claims.stream()
                .filter(predicate)
                .map(Claim::value)
                .toList();
    }

    public boolean hasClaim(Predicate<Claim> predicate) {
        return claims.stream().anyMatch(predicate);
    }

    public boolean isInRole(String role) {
        return claims.stream()
                .anyMatch(c -> (c.type().equalsIgnoreCase(CLAIM_ROLE) || c.type().equalsIgnoreCase("roles"))
                        && c.value().equalsIgnoreCase(role));
    }
}

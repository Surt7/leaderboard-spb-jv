package com.worldpinball.leaderboard.domain.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "operator_tokens",
    indexes = {
        @Index(name = "idx_operator_tokens_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_operator_tokens_active", columnList = "is_active")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OperatorToken {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String TOKEN_NAME_EMPTY = "Token name cannot be empty.";
        public static final String TOKEN_TOO_SHORT = "Token must be at least 16 characters long.";
        public static final String DATE_CANNOT_BE_NULL = "Date cannot be null.";
    }

    @Id
    @Column(name = "token_id", nullable = false)
    private UUID tokenId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "prefix", length = 32, nullable = false)
    private String prefix;

    @Column(name = "scopes", length = 500, nullable = false)
    private String scopes = "*";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "issued_by_user_id", length = 450)
    private String issuedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "expires_at")
    private Instant expiresAtUtc;

    @Column(name = "revoked_at")
    private Instant revokedAtUtc;

    public OperatorToken(
            String name,
            String rawToken,
            String scopes,
            String issuedByUserId,
            Instant expiresAtUtc,
            Instant nowUtc
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.TOKEN_NAME_EMPTY);
        }
        if (rawToken == null || rawToken.isBlank() || rawToken.length() < 16) {
            throw new IllegalArgumentException(ErrorMessages.TOKEN_TOO_SHORT);
        }

        Instant now = nowUtc != null ? nowUtc : Instant.now();

        this.tokenId = UUID.randomUUID();
        this.name = name.trim();
        this.tokenHash = hashToken(rawToken);
        this.prefix = rawToken.length() >= 8 ? rawToken.substring(0, 8) + "..." : rawToken;
        this.scopes = (scopes == null || scopes.isBlank()) ? "*" : scopes.trim();
        this.active = true;
        this.issuedByUserId = issuedByUserId;
        this.createdAtUtc = now;
        this.expiresAtUtc = expiresAtUtc;
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public List<String> getScopeList() {
        return Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public boolean isValid(Instant nowUtc) {
        if (!active) return false;
        if (revokedAtUtc != null && !revokedAtUtc.isAfter(nowUtc)) return false;
        if (expiresAtUtc != null && !expiresAtUtc.isAfter(nowUtc)) return false;
        return true;
    }

    public boolean hasScope(String requiredScope) {
        if (requiredScope == null || requiredScope.isBlank()) return true;
        if ("*".equals(scopes)) return true;

        return getScopeList().stream()
                .anyMatch(s -> "*".equalsIgnoreCase(s) || requiredScope.equalsIgnoreCase(s));
    }

    public void revoke(Instant nowUtc) {
        if (nowUtc == null) {
            throw new IllegalArgumentException(ErrorMessages.DATE_CANNOT_BE_NULL);
        }
        this.active = false;
        this.revokedAtUtc = nowUtc;
    }
}

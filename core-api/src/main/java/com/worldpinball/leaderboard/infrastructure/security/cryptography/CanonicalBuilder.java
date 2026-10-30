package com.worldpinball.leaderboard.infrastructure.security.cryptography;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Builds the canonical byte payload that is signed and verified for machine requests.
 * Format: {@code {ts}.{nonce}.{METHOD}.{path}.{bodySha256HexLower}}
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.Cryptography.CanonicalBuilder}.
 */
@Component
public class CanonicalBuilder {

    public byte[] build(long ts, String nonce, String method, String path, String rawBody) {
        if (ts <= 0) {
            throw new IllegalArgumentException("ts must be positive");
        }
        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException("missing nonce");
        }
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("missing method");
        }

        String normalizedMethod = method.toUpperCase(Locale.ROOT);
        String normalizedPath = path != null ? path : "";
        byte[] bodyBytes = (rawBody != null ? rawBody : "").getBytes(StandardCharsets.UTF_8);
        String bodyHashHexLower = sha256HexLower(bodyBytes);

        String canonical = ts + "." + nonce + "." + normalizedMethod + "." + normalizedPath + "." + bodyHashHexLower;
        return canonical.getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256HexLower(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

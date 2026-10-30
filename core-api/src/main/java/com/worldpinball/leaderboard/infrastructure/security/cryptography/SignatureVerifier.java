package com.worldpinball.leaderboard.infrastructure.security.cryptography;

public interface SignatureVerifier {
    boolean verify(String publicKeyPem, byte[] data, String signatureBase64, String algorithm);

    default boolean verify(String publicKeyPem, byte[] data, String signatureBase64) {
        return verify(publicKeyPem, data, signatureBase64, null);
    }
}

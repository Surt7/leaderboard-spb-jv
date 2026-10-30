package com.worldpinball.leaderboard.application.security.secrets;

public record SecretGenerationResult(
        String plainBase64,
        String hashHex,
        String encrypted
) {
    public SecretGenerationResult(String plainBase64, String hashHex) {
        this(plainBase64, hashHex, "");
    }
}

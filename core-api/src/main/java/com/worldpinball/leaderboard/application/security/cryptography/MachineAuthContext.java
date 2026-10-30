package com.worldpinball.leaderboard.application.security.cryptography;

/**
 * Encapsulates the raw context extracted from a signed machine HTTP request.
 * Free of Servlet dependencies for pure unit testability.
 * Mirrors C# {@code WorldPinball.Application.Security.Cryptography.MachineAuthContext}.
 */
public record MachineAuthContext(
        String deviceIdHeader,
        String timestampHeader,
        String nonceHeader,
        String signatureHeader,
        String httpMethod,
        String requestPath,
        String rawBody
) {
}

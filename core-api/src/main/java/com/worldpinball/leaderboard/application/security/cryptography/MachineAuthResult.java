package com.worldpinball.leaderboard.application.security.cryptography;

import java.util.UUID;

/**
 * Discriminated union (Java 21 sealed interface) representing all possible outcomes
 * of machine request authentication.
 * Mirrors C# {@code WorldPinball.Application.Security.Cryptography.MachineAuthResult}.
 */
public sealed interface MachineAuthResult {

    record Success(
            UUID machineId,
            UUID deviceId,
            String nonce,
            long timestamp
    ) implements MachineAuthResult {}

    record InvalidHeader(String headerName, String reason) implements MachineAuthResult {}

    record TimestampOutOfWindow(long differenceSeconds) implements MachineAuthResult {}

    record DeviceNotFoundOrRevoked(UUID deviceId) implements MachineAuthResult {}

    record DeviceNotAttached(UUID deviceId) implements MachineAuthResult {}

    record MissingPublicKey(UUID deviceId) implements MachineAuthResult {}

    record InvalidSignature(String reason) implements MachineAuthResult {}
}

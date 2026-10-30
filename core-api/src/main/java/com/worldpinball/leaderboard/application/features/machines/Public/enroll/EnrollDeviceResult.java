package com.worldpinball.leaderboard.application.features.machines.Public.enroll;

import java.util.UUID;

public sealed interface EnrollDeviceResult {
    record Success(
            UUID deviceId,
            UUID machineId,
            String certPem,
            String certSerialNumber
    ) implements EnrollDeviceResult {}

    record InvalidToken(String reason) implements EnrollDeviceResult {}
    record DeviceAlreadyEnrolled(String serialPhysical) implements EnrollDeviceResult {}
    record MachineNotFound(UUID machineId) implements EnrollDeviceResult {}
}

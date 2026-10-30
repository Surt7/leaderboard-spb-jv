package com.worldpinball.leaderboard.application.features.machines.Public.enroll;

import java.util.UUID;

public record EnrollDeviceCommand(
        UUID machineId,
        String plainEnrollmentToken,
        String publicKeyPem,
        String deviceSerialNumber
) {
}

package com.worldpinball.leaderboard.application.features.machines.Public.enroll;

import java.util.UUID;

public record EnrollDeviceResponse(
        UUID deviceId,
        UUID machineId,
        String certPem,
        String certSerialNumber
) {
}

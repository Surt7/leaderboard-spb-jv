package com.worldpinball.leaderboard.application.features.machines.Public.enroll;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EnrollDeviceRequest(
        @NotNull(message = "MachineId is required.")
        UUID machineId,

        @NotBlank(message = "PlainEnrollmentToken is required.")
        String plainEnrollmentToken,

        @NotBlank(message = "PublicKeyPem is required.")
        String publicKeyPem,

        @NotBlank(message = "DeviceSerialNumber is required.")
        String deviceSerialNumber
) {
}

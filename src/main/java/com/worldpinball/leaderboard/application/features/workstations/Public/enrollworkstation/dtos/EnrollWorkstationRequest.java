package com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.dtos;

import jakarta.validation.constraints.NotBlank;

public record EnrollWorkstationRequest(
        @NotBlank(message = "Bootstrap token is required.")
        String plainBootstrapToken,

        @NotBlank(message = "Workstation name is required.")
        String workstationName,

        @NotBlank(message = "Public key PEM is required.")
        String publicKeyPem
) {
}

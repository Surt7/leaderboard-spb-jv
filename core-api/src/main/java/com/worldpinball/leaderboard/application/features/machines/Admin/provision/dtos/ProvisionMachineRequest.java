package com.worldpinball.leaderboard.application.features.machines.Admin.provision.dtos;

import jakarta.validation.constraints.NotBlank;

public record ProvisionMachineRequest(
        @NotBlank(message = "WorldPinball serial number cannot be empty.")
        String worldPinballSerialNumber,

        @NotBlank(message = "IP slug cannot be empty.")
        String ipSlug,

        @NotBlank(message = "Display name cannot be empty.")
        String displayName
) {}

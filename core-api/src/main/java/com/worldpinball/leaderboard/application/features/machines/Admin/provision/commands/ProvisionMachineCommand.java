package com.worldpinball.leaderboard.application.features.machines.Admin.provision.commands;

import java.util.UUID;

public record ProvisionMachineCommand(
        String serialNumber,
        String ipSlug,
        String displayName,
        UUID idempotencyKey,
        String issuedByUserId
) {}

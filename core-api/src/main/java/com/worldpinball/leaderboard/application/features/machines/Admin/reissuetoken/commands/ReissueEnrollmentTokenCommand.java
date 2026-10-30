package com.worldpinball.leaderboard.application.features.machines.Admin.reissuetoken.commands;

import java.util.UUID;

public record ReissueEnrollmentTokenCommand(
        UUID machineId,
        String issuedByUserId
) {}

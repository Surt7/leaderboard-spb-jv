package com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.commands;

public record EnrollWorkstationCommand(
        String plainBootstrapToken,
        String workstationName,
        String publicKeyPem
) {
}

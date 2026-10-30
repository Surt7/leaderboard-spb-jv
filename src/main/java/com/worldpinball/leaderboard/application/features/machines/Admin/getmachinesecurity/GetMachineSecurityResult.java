package com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity;

import com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity.dtos.MachineSecurityDto;

import java.util.UUID;

public sealed interface GetMachineSecurityResult {
    record Success(MachineSecurityDto security) implements GetMachineSecurityResult {}
    record MachineNotFound(UUID machineId) implements GetMachineSecurityResult {}
}

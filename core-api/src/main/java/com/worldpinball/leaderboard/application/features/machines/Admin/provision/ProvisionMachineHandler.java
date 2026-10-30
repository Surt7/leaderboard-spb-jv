package com.worldpinball.leaderboard.application.features.machines.Admin.provision;

import com.worldpinball.leaderboard.application.features.machines.Admin.provision.commands.ProvisionMachineCommand;

public interface ProvisionMachineHandler {
    ProvisionMachineResult handle(ProvisionMachineCommand command);
}

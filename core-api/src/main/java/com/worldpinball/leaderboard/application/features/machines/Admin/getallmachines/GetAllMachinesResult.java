package com.worldpinball.leaderboard.application.features.machines.Admin.getallmachines;

import java.util.List;

public sealed interface GetAllMachinesResult {
    record Success(List<MachineRowDto> machines) implements GetAllMachinesResult {}
    record NotFound() implements GetAllMachinesResult {}
}

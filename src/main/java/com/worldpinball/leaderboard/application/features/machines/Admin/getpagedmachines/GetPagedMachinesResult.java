package com.worldpinball.leaderboard.application.features.machines.Admin.getpagedmachines;

import com.worldpinball.leaderboard.application.dtos.PagedResponse;
import com.worldpinball.leaderboard.application.features.machines.Admin.getallmachines.MachineRowDto;

public sealed interface GetPagedMachinesResult {
    record Success(PagedResponse<MachineRowDto> page) implements GetPagedMachinesResult {}
    record NotFound() implements GetPagedMachinesResult {}
}

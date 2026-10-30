package com.worldpinball.leaderboard.application.features.machines.Admin.getpagedmachines;

import com.worldpinball.leaderboard.application.features.machines.Admin.getpagedmachines.queries.GetPagedMachinesQuery;

public interface GetPagedMachinesHandler {
    GetPagedMachinesResult handle(GetPagedMachinesQuery query);
}

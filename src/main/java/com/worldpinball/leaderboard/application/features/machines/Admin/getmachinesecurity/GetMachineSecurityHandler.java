package com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity;

import com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity.queries.GetMachineSecurityQuery;

public interface GetMachineSecurityHandler {
    GetMachineSecurityResult handle(GetMachineSecurityQuery query);
}

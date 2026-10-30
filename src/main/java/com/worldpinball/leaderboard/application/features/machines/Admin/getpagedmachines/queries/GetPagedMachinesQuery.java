package com.worldpinball.leaderboard.application.features.machines.Admin.getpagedmachines.queries;

import java.util.UUID;

public record GetPagedMachinesQuery(
        int pageSize,
        UUID lastMachineId,
        Integer pageNumber,
        String search
) {}

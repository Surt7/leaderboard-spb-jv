package com.worldpinball.leaderboard.application.features.leaderboard.Admin.dashboard;

import java.util.UUID;

public sealed interface GetMachineDashboardResult {
    record Success(MachineDashboardDto stats) implements GetMachineDashboardResult {}
    record NotFound(UUID machineId) implements GetMachineDashboardResult {}
}

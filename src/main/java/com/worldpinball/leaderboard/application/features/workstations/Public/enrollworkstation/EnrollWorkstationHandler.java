package com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation;

import com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.commands.EnrollWorkstationCommand;

public interface EnrollWorkstationHandler {
    EnrollWorkstationResult handle(EnrollWorkstationCommand command);
}

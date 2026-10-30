package com.worldpinball.leaderboard.application.features.machines.Admin.reissuetoken;

import com.worldpinball.leaderboard.application.features.machines.Admin.reissuetoken.commands.ReissueEnrollmentTokenCommand;

public interface ReissueEnrollmentTokenHandler {
    ReissueEnrollmentTokenResult handle(ReissueEnrollmentTokenCommand command);
}

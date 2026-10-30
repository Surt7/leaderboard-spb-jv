package com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken;

import com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.commands.CreateBootstrapTokenCommand;

public interface CreateBootstrapTokenHandler {
    CreateBootstrapTokenResult handle(CreateBootstrapTokenCommand command);
}

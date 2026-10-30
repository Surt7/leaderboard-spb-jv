package com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession.commands.LaunchSessionCommand;

public interface LaunchSessionHandler {
    LaunchSessionResult handle(LaunchSessionCommand command);
}

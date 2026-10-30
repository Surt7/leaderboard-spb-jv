package com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession.commands.CreateGameSessionCommand;

public interface CreateGameSessionHandler {
    CreateGameSessionResult handle(CreateGameSessionCommand command);
}

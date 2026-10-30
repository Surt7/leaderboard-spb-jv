package com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession.commands.AbortSessionCommand;

public interface AbortSessionHandler {
    AbortSessionResult handle(AbortSessionCommand command);
}

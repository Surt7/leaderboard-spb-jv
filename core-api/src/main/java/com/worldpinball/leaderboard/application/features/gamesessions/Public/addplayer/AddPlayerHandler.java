package com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.commands.AddPlayerCommand;

public interface AddPlayerHandler {
    AddPlayerResult handle(AddPlayerCommand command);
}

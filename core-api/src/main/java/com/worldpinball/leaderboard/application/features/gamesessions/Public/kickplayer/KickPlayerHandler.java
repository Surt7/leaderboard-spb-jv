package com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer.commands.KickPlayerCommand;

public interface KickPlayerHandler {
    KickPlayerResult handle(KickPlayerCommand command);
}

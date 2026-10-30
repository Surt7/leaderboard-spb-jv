package com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.commands.SubmitScoreCommand;

public interface SubmitScoreHandler {
    SubmitScoreResult handle(SubmitScoreCommand command);
}

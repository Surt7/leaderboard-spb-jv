package com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore.commands.RecordBallScoreCommand;

public interface RecordBallScoreHandler {
    RecordBallScoreResult handle(RecordBallScoreCommand command);
}

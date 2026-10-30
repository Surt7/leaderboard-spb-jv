package com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue;

public interface JoinMatchmakingQueueHandler {
    JoinMatchmakingQueueResult handle(JoinMatchmakingQueueCommand command);
}

package com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue;

public interface LeaveMatchmakingQueueHandler {
    LeaveMatchmakingQueueResult handle(LeaveMatchmakingQueueCommand command);
}

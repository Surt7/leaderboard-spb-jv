package com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue;

public sealed interface LeaveMatchmakingQueueResult {
    record Success() implements LeaveMatchmakingQueueResult {}
    record PlayerNotInQueue() implements LeaveMatchmakingQueueResult {}
}

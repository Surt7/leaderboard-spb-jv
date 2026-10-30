package com.worldpinball.leaderboard.domain.enums;

public enum GameSessionState {
    NONE,
    CREATED,
    PENDING,
    SEARCHING_PLAYER,
    MATCH_FOUND,
    JOINED,
    STARTED,
    IN_GAME,
    PAUSED,
    RESUMED,
    ENDED,
    CANCELLED,
    ABANDONED,
    ABORTED
}
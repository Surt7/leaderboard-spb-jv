package com.worldpinball.leaderboard.domain.enums;

public enum GameSlotStatus {
    Open(0),
    Occupied(1),
    Locked(2);

    private final int value;

    GameSlotStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

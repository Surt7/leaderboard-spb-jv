package com.worldpinball.leaderboard.domain.enums;

public enum CaType {
    Root(0),
    Intermediate(1);

    private final int value;

    CaType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

package com.worldpinball.leaderboard.domain.enums;

public enum RevocationReason {
    KeyCompromise(0),
    DeviceCompromise(1),
    AffiliationChanged(2),
    CessationOfOperation(3),
    Superseded(4),
    Unspecified(5);

    private final int value;

    RevocationReason(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

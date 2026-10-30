package com.worldpinball.leaderboard.domain.enums;

public enum DeviceCertificateStatus {
    Active(0),
    Expired(1),
    Revoked(2),
    Superseded(3);

    private final int value;

    DeviceCertificateStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

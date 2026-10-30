package com.worldpinball.leaderboard.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkstationType {
    AdminWorkstation(1),
    FieldTerminal(2);

    private final int value;

    WorkstationType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static WorkstationType fromValue(Object raw) {
        if (raw == null) {
            return AdminWorkstation;
        }
        if (raw instanceof Number n) {
            int v = n.intValue();
            for (WorkstationType wt : values()) {
                if (wt.value == v) {
                    return wt;
                }
            }
        }
        String str = raw.toString().trim();
        for (WorkstationType wt : values()) {
            if (wt.name().equalsIgnoreCase(str) || String.valueOf(wt.value).equals(str)) {
                return wt;
            }
        }
        return AdminWorkstation;
    }
}

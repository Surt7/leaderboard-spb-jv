package com.worldpinball.leaderboard.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MachineStatus {
    ACTIVE(1),
    REVOKED(2),
    INACTIVE(3),
    MAINTENANCE(4),
    PENDING_ENROLLMENT(5);

    private final int value;

    MachineStatus(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static MachineStatus fromJson(Object input) {
        if (input instanceof Number n) {
            int code = n.intValue();
            for (MachineStatus status : values()) {
                if (status.value == code) {
                    return status;
                }
            }
        } else if (input instanceof String s) {
            String normalized = s.trim();
            try {
                int code = Integer.parseInt(normalized);
                for (MachineStatus status : values()) {
                    if (status.value == code) {
                        return status;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
            for (MachineStatus status : values()) {
                if (status.name().equalsIgnoreCase(normalized)
                        || status.name().replace("_", "").equalsIgnoreCase(normalized.replace("_", ""))) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("Unknown MachineStatus: " + input);
    }
}

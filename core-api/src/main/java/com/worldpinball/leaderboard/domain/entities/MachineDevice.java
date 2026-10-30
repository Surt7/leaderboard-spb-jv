package com.worldpinball.leaderboard.domain.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machine_devices")
@IdClass(MachineDeviceId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MachineDevice {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String INVALID_MACHINE_ID = "MachineId cannot be null.";
        public static final String INVALID_DEVICE_ID = "DeviceId cannot be null.";
        public static final String INVALID_DATE = "Date cannot be null.";
        public static final String ALREADY_DETACHED = "Device is already detached.";
    }

    @Id
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Id
    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Column(name = "attached_at_utc", nullable = false)
    private Instant attachedAtUtc;

    @Column(name = "detached_at_utc")
    private Instant detachedAtUtc;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public MachineDevice(UUID machineId, UUID deviceId, Instant attachedAtUtc) {
        if (machineId == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_MACHINE_ID);
        }
        if (deviceId == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DEVICE_ID);
        }
        if (attachedAtUtc == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DATE);
        }

        this.machineId = machineId;
        this.deviceId = deviceId;
        this.attachedAtUtc = attachedAtUtc;
        this.active = true;
    }

    public void detach(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DATE);
        }
        if (!this.active) {
            throw new IllegalStateException(ErrorMessages.ALREADY_DETACHED);
        }

        this.active = false;
        this.detachedAtUtc = now;
    }
}

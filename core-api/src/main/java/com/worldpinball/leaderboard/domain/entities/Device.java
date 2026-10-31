package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Device {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String EMPTY_SERIAL_PHYSICAL = "Physical serial number cannot be empty.";
        public static final String EMPTY_PUBLIC_KEY = "Public key cannot be empty.";
        public static final String DEVICE_ALREADY_REVOKED = "A revoked device cannot perform operations or be reactivated.";
        public static final String DEVICE_NOT_ACTIVE = "Device must be in Active status to perform this action.";
        public static final String COUNTER_MUST_BE_STRICTLY_INCREASING = "Counter value %d is invalid. It must be strictly greater than last counter %d.";
    }

    @Id
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "serial_physical", nullable = false)
    private String serialPhysical;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DeviceStatus status = DeviceStatus.PENDING;

    @Column(name = "public_key")
    private String publicKey;

    @Column(name = "public_key_fingerprint")
    private String publicKeyFingerprint;

    @Column(name = "key_type")
    private String keyType;

    @Column(name = "key_algorithm")
    private String keyAlgorithm;

    @Column(name = "last_counter", nullable = false)
    private long lastCounter = 0;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "provisioned_at_utc")
    private Instant provisionedAtUtc;

    @Column(name = "activated_at_utc")
    private Instant activatedAtUtc;

    @Column(name = "revoked_at_utc")
    private Instant revokedAtUtc;

    @Column(name = "last_seen_at_utc")
    private Instant lastSeenAtUtc;

    @Column(name = "client_version")
    private String clientVersion;

    @Column(name = "notes")
    private String notes;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    public Device(
        String serialPhysical,
        Instant createdAtUtc,
        UUID deviceId,
        String clientVersion,
        String notes
    ) {
        if (serialPhysical == null || serialPhysical.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_SERIAL_PHYSICAL);
        }

        this.deviceId = deviceId != null ? deviceId : UUID.randomUUID();
        this.serialPhysical = serialPhysical.trim();
        this.status = DeviceStatus.PENDING;
        this.createdAtUtc = createdAtUtc != null ? createdAtUtc : Instant.now();
        this.clientVersion = clientVersion;
        this.notes = notes;
        this.lastCounter = 0;
    }

    public static Device enroll(
        String serialPhysical,
        String publicKeyPem,
        Instant enrolledAtUtc
    ) {
        return enroll(serialPhysical, publicKeyPem, enrolledAtUtc, null, UUID.randomUUID());
    }

    public static Device enroll(
        String serialPhysical,
        String publicKeyPem,
        Instant enrolledAtUtc,
        String keyAlgorithm,
        UUID deviceId
    ) {
        Device device = new Device(serialPhysical, enrolledAtUtc, deviceId, null, null);
        device.activate(publicKeyPem, keyAlgorithm, enrolledAtUtc);
        return device;
    }

    public static Device createActive(
        String serialPhysical,
        String publicKey,
        String keyAlgorithm,
        Instant activatedAtUtc,
        UUID deviceId
    ) {
        Device device = new Device(serialPhysical, activatedAtUtc, deviceId, null, null);
        device.activate(publicKey, keyAlgorithm, activatedAtUtc);
        return device;
    }

    public void activate(String publicKey, String keyAlgorithm, Instant activatedAtUtc) {
        if (this.status == DeviceStatus.REVOKED) {
            throw new IllegalStateException(ErrorMessages.DEVICE_ALREADY_REVOKED);
        }
        if (publicKey == null || publicKey.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_PUBLIC_KEY);
        }

        this.publicKey = publicKey.trim();
        this.keyAlgorithm = keyAlgorithm != null ? keyAlgorithm.trim() : null;
        this.status = DeviceStatus.ACTIVE;
        this.activatedAtUtc = activatedAtUtc != null ? activatedAtUtc : Instant.now();
        this.updatedAtUtc = this.activatedAtUtc;
    }

    public void revoke(Instant revokedAtUtc, String reason) {
        if (this.status == DeviceStatus.REVOKED) {
            throw new IllegalStateException(ErrorMessages.DEVICE_ALREADY_REVOKED);
        }

        this.status = DeviceStatus.REVOKED;
        this.revokedAtUtc = revokedAtUtc != null ? revokedAtUtc : Instant.now();
        this.updatedAtUtc = this.revokedAtUtc;

        if (reason != null && !reason.isBlank()) {
            this.notes = (this.notes == null || this.notes.isBlank())
                ? "Revoked: " + reason.trim()
                : this.notes + " | Revoked: " + reason.trim();
        }
    }

    public void verifyAndIncrementCounter(long incomingCounter) {
        if (this.status == DeviceStatus.REVOKED) {
            throw new IllegalStateException(ErrorMessages.DEVICE_ALREADY_REVOKED);
        }
        if (this.status != DeviceStatus.ACTIVE) {
            throw new IllegalStateException(ErrorMessages.DEVICE_NOT_ACTIVE);
        }
        if (incomingCounter <= this.lastCounter) {
            throw new IllegalStateException(
                String.format(ErrorMessages.COUNTER_MUST_BE_STRICTLY_INCREASING, incomingCounter, this.lastCounter)
            );
        }
        this.lastCounter = incomingCounter;
    }

    public void recordHeartbeat(Instant lastSeenAtUtc, String clientVersion) {
        if (this.status == DeviceStatus.REVOKED) {
            throw new IllegalStateException(ErrorMessages.DEVICE_ALREADY_REVOKED);
        }

        this.lastSeenAtUtc = lastSeenAtUtc != null ? lastSeenAtUtc : Instant.now();
        if (clientVersion != null && !clientVersion.isBlank()) {
            this.clientVersion = clientVersion.trim();
        }
        this.updatedAtUtc = this.lastSeenAtUtc;
    }

    public void setPublicKeyFingerprint(String fingerprint) {
        this.publicKeyFingerprint = fingerprint;
    }

    public void assignOwner(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Owner user ID cannot be null.");
        }
        this.ownerUserId = userId;
        this.updatedAtUtc = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID deviceId;
        private UUID ownerUserId;
        private String serialPhysical;
        private DeviceStatus status = DeviceStatus.PENDING;
        private String publicKey;
        private String publicKeyFingerprint;
        private String keyType;
        private String keyAlgorithm;
        private long lastCounter = 0;
        private Instant createdAtUtc;
        private Instant provisionedAtUtc;
        private Instant activatedAtUtc;
        private Instant revokedAtUtc;
        private Instant lastSeenAtUtc;
        private String clientVersion;
        private String notes;
        private Instant updatedAtUtc;

        public Builder deviceId(UUID deviceId) { this.deviceId = deviceId; return this; }
        public Builder ownerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; return this; }
        public Builder serialPhysical(String serialPhysical) { this.serialPhysical = serialPhysical; return this; }
        public Builder status(DeviceStatus status) { this.status = status; return this; }
        public Builder publicKey(String publicKey) { this.publicKey = publicKey; return this; }
        public Builder publicKeyFingerprint(String fingerprint) { this.publicKeyFingerprint = fingerprint; return this; }
        public Builder keyType(String keyType) { this.keyType = keyType; return this; }
        public Builder keyAlgorithm(String keyAlgorithm) { this.keyAlgorithm = keyAlgorithm; return this; }
        public Builder lastCounter(long counter) { this.lastCounter = counter; return this; }
        public Builder createdAtUtc(Instant createdAtUtc) { this.createdAtUtc = createdAtUtc; return this; }
        public Builder provisionedAtUtc(Instant provisionedAtUtc) { this.provisionedAtUtc = provisionedAtUtc; return this; }
        public Builder activatedAtUtc(Instant activatedAtUtc) { this.activatedAtUtc = activatedAtUtc; return this; }
        public Builder revokedAtUtc(Instant revokedAtUtc) { this.revokedAtUtc = revokedAtUtc; return this; }
        public Builder lastSeenAtUtc(Instant lastSeenAtUtc) { this.lastSeenAtUtc = lastSeenAtUtc; return this; }
        public Builder clientVersion(String clientVersion) { this.clientVersion = clientVersion; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder updatedAtUtc(Instant updatedAtUtc) { this.updatedAtUtc = updatedAtUtc; return this; }

        public Device build() {
            Device device = new Device(
                serialPhysical,
                createdAtUtc != null ? createdAtUtc : Instant.now(),
                deviceId != null ? deviceId : UUID.randomUUID(),
                clientVersion,
                notes
            );
            if (ownerUserId != null) device.ownerUserId = ownerUserId;
            if (status != null) device.status = status;
            if (publicKey != null) device.publicKey = publicKey;
            if (publicKeyFingerprint != null) device.publicKeyFingerprint = publicKeyFingerprint;
            if (keyType != null) device.keyType = keyType;
            if (keyAlgorithm != null) device.keyAlgorithm = keyAlgorithm;
            device.lastCounter = lastCounter;
            if (provisionedAtUtc != null) device.provisionedAtUtc = provisionedAtUtc;
            if (activatedAtUtc != null) device.activatedAtUtc = activatedAtUtc;
            if (revokedAtUtc != null) device.revokedAtUtc = revokedAtUtc;
            if (lastSeenAtUtc != null) device.lastSeenAtUtc = lastSeenAtUtc;
            if (updatedAtUtc != null) device.updatedAtUtc = updatedAtUtc;
            return device;
        }
    }
}

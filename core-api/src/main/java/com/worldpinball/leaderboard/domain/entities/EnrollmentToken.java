package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.EnrollmentTokenType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enrollment_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnrollmentToken {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String TOKEN_HASH = "The provided token hash is invalid / empty.";
        public static final String TOKEN_TYPE = "Invalid token type.";
        public static final String ISSUED_BY_USER_ID = "The provided issued by is invalid / empty.";
        public static final String ISSUED_AT = "Issued date cannot be empty.";
        public static final String EXPIRES_AT = "Expiration date must be after issued date.";
        public static final String REVOKED_AT = "Token revoked.";
        public static final String EXPIRED_AT = "Token expired.";
        public static final String ALREADY_CONSUMED = "Token already consumed.";
        public static final String DIFFERENT_DEVICE = "Token has different device Id";
        public static final String INVALID_DEVICE_ID = "Device Id cannot be empty.";
        public static final String INVALID_DATE = "Date cannot be default.";
        public static final String INVALID_VALIDITY_HOURS = "Validity hours must be greater than zero.";
    }

    public static final int DEFAULT_VALIDITY_HOURS = 24;

    @Id
    @Column(name = "token_id", nullable = false)
    private UUID tokenId;

    @Column(name = "token_hash", length = 256, nullable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", length = 20, nullable = false)
    private EnrollmentTokenType tokenType = EnrollmentTokenType.DeviceEnroll;

    @Column(name = "machine_id")
    private UUID machineId;

    @Column(name = "device_id")
    private UUID deviceId;

    @Column(name = "issued_by_user_id", length = 450, nullable = false)
    private String issuedByUserId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAtUtc;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAtUtc;

    @Column(name = "consumed_at")
    private Instant consumedAtUtc;

    @Column(name = "consumed_by_device_id")
    private UUID consumedByDeviceId;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "max_uses", nullable = false)
    private int maxUses = 1;

    @Column(name = "uses_count", nullable = false)
    private int usesCount = 0;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "validity_hours")
    private Integer validityHours;

    public EnrollmentToken(
            String tokenHash,
            EnrollmentTokenType tokenType,
            String issuedByUserId,
            Instant issuedAt,
            Instant expiresAt,
            UUID machineId
    ) {
        this(tokenHash, tokenType, issuedByUserId, issuedAt, expiresAt, machineId, null, 1, null, DEFAULT_VALIDITY_HOURS);
    }

    public EnrollmentToken(
            String tokenHash,
            EnrollmentTokenType tokenType,
            String issuedByUserId,
            Instant issuedAt,
            Instant expiresAt,
            UUID machineId,
            UUID deviceId,
            int maxUses,
            String notes,
            Integer validityHours
    ) {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.TOKEN_HASH);
        }
        if (tokenType == null) {
            throw new IllegalArgumentException(ErrorMessages.TOKEN_TYPE);
        }
        if (issuedByUserId == null || issuedByUserId.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.ISSUED_BY_USER_ID);
        }
        if (issuedAt == null) {
            throw new IllegalArgumentException(ErrorMessages.ISSUED_AT);
        }
        if (expiresAt == null || !expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(ErrorMessages.EXPIRES_AT);
        }
        if (validityHours != null && validityHours <= 0) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_VALIDITY_HOURS);
        }

        this.tokenId = UUID.randomUUID();
        this.tokenHash = tokenHash;
        this.tokenType = tokenType;
        this.issuedByUserId = issuedByUserId.trim();
        this.issuedAtUtc = issuedAt;
        this.expiresAtUtc = expiresAt;
        this.machineId = machineId;
        this.deviceId = deviceId;
        this.maxUses = maxUses <= 0 ? 1 : maxUses;
        this.notes = notes != null ? notes.trim() : null;
        this.validityHours = validityHours != null ? validityHours : DEFAULT_VALIDITY_HOURS;
        this.usesCount = 0;
    }

    public void consume(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DATE);
        }
        if (this.revokedAt != null) {
            throw new IllegalStateException(ErrorMessages.REVOKED_AT);
        }
        if (!now.isBefore(this.expiresAtUtc)) {
            throw new IllegalStateException(ErrorMessages.EXPIRED_AT);
        }
        if (this.usesCount >= this.maxUses) {
            throw new IllegalStateException(ErrorMessages.ALREADY_CONSUMED);
        }

        this.usesCount++;
        if (this.consumedAtUtc == null) {
            this.consumedAtUtc = now;
        }
    }

    public void consume(UUID deviceId, Instant now) {
        if (deviceId == null || deviceId.equals(new UUID(0L, 0L))) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DEVICE_ID);
        }
        if (now == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DATE);
        }
        if (this.revokedAt != null) {
            throw new IllegalStateException(ErrorMessages.REVOKED_AT);
        }
        if (!now.isBefore(this.expiresAtUtc)) {
            throw new IllegalStateException(ErrorMessages.EXPIRED_AT);
        }
        if (this.usesCount >= this.maxUses) {
            throw new IllegalStateException(ErrorMessages.ALREADY_CONSUMED);
        }
        if (this.deviceId != null && !deviceId.equals(this.deviceId)) {
            throw new IllegalStateException(ErrorMessages.DIFFERENT_DEVICE);
        }

        this.usesCount++;
        if (this.consumedAtUtc == null) {
            this.consumedAtUtc = now;
        }
        this.consumedByDeviceId = deviceId;
    }

    public void revoke(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DATE);
        }
        this.revokedAt = now;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID tokenId;
        private String tokenHash;
        private EnrollmentTokenType tokenType = EnrollmentTokenType.DeviceEnroll;
        private UUID machineId;
        private UUID deviceId;
        private String issuedByUserId;
        private Instant issuedAtUtc;
        private Instant expiresAtUtc;
        private Instant consumedAtUtc;
        private UUID consumedByDeviceId;
        private Instant revokedAt;
        private int maxUses = 1;
        private int usesCount = 0;
        private String notes;
        private Integer validityHours;

        public Builder tokenId(UUID tokenId) { this.tokenId = tokenId; return this; }
        public Builder tokenHash(String tokenHash) { this.tokenHash = tokenHash; return this; }
        public Builder tokenType(EnrollmentTokenType tokenType) { this.tokenType = tokenType; return this; }
        public Builder machineId(UUID machineId) { this.machineId = machineId; return this; }
        public Builder deviceId(UUID deviceId) { this.deviceId = deviceId; return this; }
        public Builder issuedByUserId(String issuedByUserId) { this.issuedByUserId = issuedByUserId; return this; }
        public Builder issuedAtUtc(Instant issuedAtUtc) { this.issuedAtUtc = issuedAtUtc; return this; }
        public Builder expiresAtUtc(Instant expiresAtUtc) { this.expiresAtUtc = expiresAtUtc; return this; }
        public Builder consumedAtUtc(Instant consumedAtUtc) { this.consumedAtUtc = consumedAtUtc; return this; }
        public Builder consumedByDeviceId(UUID consumedByDeviceId) { this.consumedByDeviceId = consumedByDeviceId; return this; }
        public Builder revokedAt(Instant revokedAt) { this.revokedAt = revokedAt; return this; }
        public Builder maxUses(int maxUses) { this.maxUses = maxUses; return this; }
        public Builder usesCount(int usesCount) { this.usesCount = usesCount; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder validityHours(Integer validityHours) { this.validityHours = validityHours; return this; }

        public EnrollmentToken build() {
            EnrollmentToken token = new EnrollmentToken(
                tokenHash != null ? tokenHash : UUID.randomUUID().toString(),
                tokenType != null ? tokenType : EnrollmentTokenType.DeviceEnroll,
                issuedByUserId != null ? issuedByUserId : "system",
                issuedAtUtc != null ? issuedAtUtc : Instant.now(),
                expiresAtUtc != null ? expiresAtUtc : Instant.now().plusSeconds(86400),
                machineId != null ? machineId : UUID.randomUUID()
            );
            if (tokenId != null) token.tokenId = tokenId;
            if (deviceId != null) token.deviceId = deviceId;
            if (consumedAtUtc != null) token.consumedAtUtc = consumedAtUtc;
            if (consumedByDeviceId != null) token.consumedByDeviceId = consumedByDeviceId;
            if (revokedAt != null) token.revokedAt = revokedAt;
            token.maxUses = maxUses;
            token.usesCount = usesCount;
            if (notes != null) token.notes = notes;
            if (validityHours != null) token.validityHours = validityHours;
            return token;
        }
    }
}

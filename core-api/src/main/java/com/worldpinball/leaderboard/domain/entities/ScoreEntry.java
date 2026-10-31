package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.events.ScoreSubmittedDomainEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "score_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScoreEntry extends BaseEntity {

    public static final class ErrorMessages {
        private ErrorMessages() {} // Constructeur privé pour empêcher d'instancier la classe d'erreurs
        public static final String SESSION_ID = "SessionId cannot be empty.";
        public static final String DEVICE_ID = "DeviceId cannot be empty.";
        public static final String MACHINE_ID = "MachineId cannot be empty.";
        public static final String IP_ID = "IpId cannot be empty.";
        public static final String MODE_ID = "ModeId cannot be empty.";
        public static final String INVALID_SLOT_NUMBER = "Slot number must be between 1 and 4.";
        public static final String INVALID_SCORE = "Score cannot be negative.";
        public static final String INVALID_REASON = "A validation reason is required.";
    }


    @Id
    @Column(name = "score_entry_id", nullable = false)
    private UUID scoreEntryId;

    @Column(name = "slot_number", nullable = false)
    private int slotNumber;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Column(name = "ip_id", nullable = false)
    private UUID ipId;

    @Column(name = "mode_id", nullable = false)
    private UUID modeId;

    @Column(name = "score", nullable = false)
    private long score;

    @Column(name = "achieved_at_utc", nullable = false)
    private Instant achievedAtUtc;

    @Column(name = "is_validated", nullable = false)
    private boolean validated = true;

    @Column(name = "validation_reason")
    private String validationReason;


    public ScoreEntry(
            UUID sessionId,
            int slotNumber,
            UUID deviceId,
            UUID machineId,
            UUID ipId,
            UUID modeId,
            long score,
            Instant achievedAtUtc,
            UUID userId
    ) {
        if (sessionId == null) {
            throw new IllegalArgumentException(ErrorMessages.SESSION_ID);
        }
        if (deviceId == null) {
            throw new IllegalArgumentException(ErrorMessages.DEVICE_ID);
        }
        if (machineId == null) {
            throw new IllegalArgumentException(ErrorMessages.MACHINE_ID);
        }
        if (ipId == null) {
            throw new IllegalArgumentException(ErrorMessages.IP_ID);
        }
        if (modeId == null) {
            throw new IllegalArgumentException(ErrorMessages.MODE_ID);
        }
        if (slotNumber < 1 || slotNumber > 4) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_SLOT_NUMBER);
        }
        if (score < 0) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_SCORE);
        }

        this.scoreEntryId = UUID.randomUUID();
        this.sessionId = sessionId;
        this.slotNumber = slotNumber;
        this.deviceId = deviceId;
        this.machineId = machineId;
        this.ipId = ipId;
        this.modeId = modeId;
        this.score = score;
        this.achievedAtUtc = achievedAtUtc != null ? achievedAtUtc : Instant.now();
        this.userId = userId;
        this.validated = true;

        addDomainEvent(new ScoreSubmittedDomainEvent(
                this.scoreEntryId,
                this.sessionId,
                this.machineId,
                this.ipId,
                this.modeId,
                this.slotNumber,
                this.userId != null ? this.userId.toString() : null,
                this.score,
                this.achievedAtUtc
        ));
    }


    public void invalidate(String reason){
        if(reason == null || reason.isBlank()){
            throw new IllegalArgumentException(ErrorMessages.INVALID_REASON);
        }
        this.validated = false;
        this.validationReason = reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID scoreEntryId;
        private int slotNumber = 1;
        private UUID sessionId;
        private UUID userId;
        private UUID deviceId;
        private UUID machineId;
        private UUID ipId;
        private UUID modeId;
        private long score;
        private Instant achievedAtUtc;
        private boolean validated = true;
        private String validationReason;

        public Builder scoreEntryId(UUID scoreEntryId) { this.scoreEntryId = scoreEntryId; return this; }
        public Builder slotNumber(int slotNumber) { this.slotNumber = slotNumber; return this; }
        public Builder sessionId(UUID sessionId) { this.sessionId = sessionId; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder deviceId(UUID deviceId) { this.deviceId = deviceId; return this; }
        public Builder machineId(UUID machineId) { this.machineId = machineId; return this; }
        public Builder ipId(UUID ipId) { this.ipId = ipId; return this; }
        public Builder modeId(UUID modeId) { this.modeId = modeId; return this; }
        public Builder score(long score) { this.score = score; return this; }
        public Builder achievedAtUtc(Instant achievedAtUtc) { this.achievedAtUtc = achievedAtUtc; return this; }
        public Builder validated(boolean validated) { this.validated = validated; return this; }
        public Builder validationReason(String validationReason) { this.validationReason = validationReason; return this; }

        public ScoreEntry build() {
            ScoreEntry entry = new ScoreEntry(
                sessionId,
                slotNumber,
                deviceId,
                machineId,
                ipId,
                modeId,
                score,
                achievedAtUtc != null ? achievedAtUtc : Instant.now(),
                userId
            );
            if (scoreEntryId != null) {
                entry.scoreEntryId = scoreEntryId;
            }
            entry.validated = validated;
            entry.validationReason = validationReason;
            return entry;
        }
    }
}

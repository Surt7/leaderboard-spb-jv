package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machines")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Machine {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String EMPTY_IP_ID = "The IP license identifier cannot be empty.";
        public static final String EMPTY_SERIAL_NUMBER = "The WorldPinball serial number cannot be empty.";
        public static final String EMPTY_DISPLAY_NAME = "The display name cannot be empty.";
        public static final String CANNOT_RETURN_TO_PENDING_ENROLLMENT = "A machine cannot return to pending enrollment.";
        public static final String CANNOT_COMPLETE_ENROLLMENT = "Only a machine pending enrollment can complete enrollment.";
        public static final String REVOKED_MACHINE_CANNOT_BE_REACTIVATED = "A revoked machine cannot be reactivated.";
        public static final String PENDING_ENROLLMENT_REQUIRES_COMPLETION = "A machine pending enrollment must complete enrollment before activation.";
        public static final String EMPTY_NONCE = "The nonce is mandatory.";
        public static final String EMPTY_IDEMPOTENCY_KEY = "The Idempotency Key is mandatory.";
    }

    @Id
    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Column(name = "ip_id", nullable = false)
    private UUID ipId;

    @Column(name = "worldpinball_serial_number", length = 32, nullable = false)
    private String worldPinballSerialNumber;

    @Column(name = "display_name", length = 150, nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MachineStatus status = MachineStatus.PENDING_ENROLLMENT;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "nonce", length = 64, nullable = false)
    private String nonce;

    @Column(name = "machine_notes")
    private String machineNotes;

    @Column(name = "manufacturer", length = 150)
    private String manufacturer;

    @Column(name = "manufacturer_model_code", length = 50)
    private String manufacturerModelCode;

    @Column(name = "manufacturer_model_name", length = 150)
    private String manufacturerModelName;

    @Column(name = "manufacturer_model_year", length = 10)
    private String manufacturerModelYear;

    @Column(name = "manufacturer_serial_number", length = 100)
    private String manufacturerSerialNumber;

    @Column(name = "manufacturer_notes", length = 255)
    private String manufacturerNotes;

    @Column(name = "location", length = 150)
    private String location;

    @Column(name = "voltage")
    private Integer voltage;

    @Column(name = "frequency_hz")
    private Integer frequencyHz;

    @Column(name = "amperage")
    private java.math.BigDecimal amperage;

    @Column(name = "manufactured_date")
    private java.time.LocalDate manufacturedDate;

    @Column(name = "registered_at_utc", nullable = false)
    private Instant registeredAtUtc;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    public Machine(
        UUID ipId,
        String serialNumber,
        String displayName,
        Instant registeredAtUtc,
        UUID idempotencyKey,
        String nonce,
        UUID machineId
    ) {
        if (ipId == null) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_IP_ID);
        }
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_SERIAL_NUMBER);
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_DISPLAY_NAME);
        }
        if (idempotencyKey == null) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_IDEMPOTENCY_KEY);
        }
        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_NONCE);
        }

        this.machineId = machineId != null ? machineId : UUID.randomUUID();
        this.ipId = ipId;
        this.worldPinballSerialNumber = serialNumber.trim();
        this.displayName = displayName.trim();
        this.registeredAtUtc = registeredAtUtc != null ? registeredAtUtc : Instant.now();
        this.idempotencyKey = idempotencyKey;
        this.nonce = nonce.trim();
        this.status = MachineStatus.PENDING_ENROLLMENT;
    }

    public void completeEnrollment(Instant now) {
        if (this.status != MachineStatus.PENDING_ENROLLMENT) {
            throw new IllegalStateException(ErrorMessages.CANNOT_COMPLETE_ENROLLMENT);
        }
        this.status = MachineStatus.ACTIVE;
        this.updatedAtUtc = now != null ? now : Instant.now();
    }

    public void activate(Instant now) {
        if (this.status == MachineStatus.REVOKED) {
            throw new IllegalStateException(ErrorMessages.REVOKED_MACHINE_CANNOT_BE_REACTIVATED);
        }
        if (this.status == MachineStatus.PENDING_ENROLLMENT) {
            throw new IllegalStateException(ErrorMessages.PENDING_ENROLLMENT_REQUIRES_COMPLETION);
        }
        this.status = MachineStatus.ACTIVE;
        this.updatedAtUtc = now != null ? now : Instant.now();
    }

    public void setStatus(MachineStatus status, Instant now) {
        if (status == null) {
            throw new IllegalArgumentException("MachineStatus cannot be null.");
        }
        if (status == MachineStatus.ACTIVE) {
            activate(now);
            return;
        }
        if (status == MachineStatus.PENDING_ENROLLMENT && this.status != MachineStatus.PENDING_ENROLLMENT) {
            throw new IllegalStateException(ErrorMessages.CANNOT_RETURN_TO_PENDING_ENROLLMENT);
        }
        this.status = status;
        this.updatedAtUtc = now != null ? now : Instant.now();
    }
}

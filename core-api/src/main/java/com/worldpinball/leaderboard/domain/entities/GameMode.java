package com.worldpinball.leaderboard.domain.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_modes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameMode {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String INVALID_CODE = "Mode code cannot be empty.";
        public static final String INVALID_NAME = "Mode name cannot be empty.";
        public static final String INVALID_DATE = "Date cannot be null.";
    }

    @Id
    @Column(name = "mode_id", nullable = false)
    private UUID modeId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    public GameMode(
        String code,
        String name,
        String description,
        boolean active,
        Instant createdAtUtc,
        UUID modeId
    ) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_CODE);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_NAME);
        }

        this.modeId = modeId != null ? modeId : UUID.randomUUID();
        this.code = code.trim().toLowerCase();
        this.name = name.trim();
        this.description = description != null ? description.trim() : null;
        this.active = active;
        this.createdAtUtc = createdAtUtc != null ? createdAtUtc : Instant.now();
    }

    public GameMode(String code, String name, String description, boolean active, UUID modeId) {
        this(code, name, description, active, Instant.now(), modeId);
    }

    public void updateDetails(String name, String description, Instant now) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_NAME);
        }
        if (now == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DATE);
        }

        this.name = name.trim();
        this.description = description != null ? description.trim() : null;
        this.updatedAtUtc = now;
    }

    public void activate(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DATE);
        }
        this.active = true;
        this.updatedAtUtc = now;
    }

    public void deactivate(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DATE);
        }
        this.active = false;
        this.updatedAtUtc = now;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID modeId;
        private String code;
        private String name;
        private String description;
        private boolean active = true;
        private Instant createdAtUtc;
        private Instant updatedAtUtc;

        public Builder modeId(UUID modeId) { this.modeId = modeId; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAtUtc(Instant createdAtUtc) { this.createdAtUtc = createdAtUtc; return this; }
        public Builder updatedAtUtc(Instant updatedAtUtc) { this.updatedAtUtc = updatedAtUtc; return this; }

        public GameMode build() {
            GameMode mode = new GameMode(
                code,
                name,
                description,
                active,
                createdAtUtc != null ? createdAtUtc : Instant.now(),
                modeId != null ? modeId : UUID.randomUUID()
            );
            if (updatedAtUtc != null) mode.updatedAtUtc = updatedAtUtc;
            return mode;
        }
    }
}

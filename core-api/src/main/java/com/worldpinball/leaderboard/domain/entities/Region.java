package com.worldpinball.leaderboard.domain.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "regions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String INVALID_CODE = "Region code cannot be empty.";
        public static final String INVALID_NAME = "Region name cannot be empty.";
        public static final String INVALID_DATE = "Date cannot be null.";
    }

    @Id
    @Column(name = "region_id", nullable = false)
    private UUID regionId;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    public Region(String code, String name, Instant createdAtUtc) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_CODE);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_NAME);
        }

        this.regionId = UUID.randomUUID();
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.createdAtUtc = createdAtUtc != null ? createdAtUtc : Instant.now();
    }

    public void update(String name, Instant now) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_NAME);
        }
        if (now == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DATE);
        }

        this.name = name.trim();
        this.updatedAtUtc = now;
    }
}

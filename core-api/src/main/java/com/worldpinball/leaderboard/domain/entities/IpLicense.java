package com.worldpinball.leaderboard.domain.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ip_licenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IpLicense {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String INVALID_NAME = "IP License name cannot be empty.";
        public static final String INVALID_SLUG = "IP License slug cannot be empty.";
        public static final String INVALID_DATE = "Date cannot be null.";
    }

    @Id
    @Column(name = "ip_id", nullable = false)
    private UUID ipId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "publisher_id")
    private UUID publisherId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    public IpLicense(
        String name,
        String slug,
        UUID publisherId,
        boolean active,
        Instant createdAtUtc,
        UUID ipId
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_NAME);
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_SLUG);
        }

        this.ipId = ipId != null ? ipId : UUID.randomUUID();
        this.name = name.trim();
        this.slug = slug.trim().toLowerCase();
        this.publisherId = publisherId;
        this.active = active;
        this.createdAtUtc = createdAtUtc != null ? createdAtUtc : Instant.now();
    }

    public IpLicense(String name, String slug, boolean active, UUID ipId) {
        this(name, slug, null, active, Instant.now(), ipId);
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

    public void setPublisher(UUID publisherId, Instant now) {
        if (now == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DATE);
        }
        this.publisherId = publisherId;
        this.updatedAtUtc = now;
    }

    public void updateDetails(String name, String slug, String notes, Instant now) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_NAME);
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_SLUG);
        }
        if (now == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DATE);
        }

        this.name = name.trim();
        this.slug = slug.trim().toLowerCase();
        this.notes = notes != null ? notes.trim() : null;
        this.updatedAtUtc = now;
    }
}

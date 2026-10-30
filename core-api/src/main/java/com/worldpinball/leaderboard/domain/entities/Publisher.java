package com.worldpinball.leaderboard.domain.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "publishers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Publisher {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String INVALID_NAME = "Publisher name cannot be empty.";
        public static final String INVALID_SLUG = "Publisher slug cannot be empty.";
        public static final String INVALID_DATE = "Date cannot be null.";
    }

    @Id
    @Column(name = "publisher_id", nullable = false)
    private UUID publisherId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    public Publisher(String name, String slug, Instant createdAtUtc) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_NAME);
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_SLUG);
        }

        this.publisherId = UUID.randomUUID();
        this.name = name.trim();
        this.slug = slug.trim().toLowerCase();
        this.createdAtUtc = createdAtUtc != null ? createdAtUtc : Instant.now();
    }

    public Publisher(String name, String slug, Instant createdAtUtc, UUID publisherId) {
        this(name, slug, createdAtUtc);
        if (publisherId != null) {
            this.publisherId = publisherId;
        }
    }

    public void update(String name, String slug, Instant now) {
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
        this.updatedAtUtc = now;
    }
}

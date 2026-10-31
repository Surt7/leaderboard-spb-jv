package com.worldpinball.leaderboard.domain.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    private static final Set<String> RESERVED_DISPLAY_NAMES = Set.of(
        "admin", "administrator", "system", "worldpinball", "no name", "anonymous"
    );

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String INVALID_USER_ID = "UserId cannot be empty.";
        public static final String INVALID_DISPLAY_NAME = "DisplayName cannot be empty.";
        public static final String RESERVED_DISPLAY_NAME = "DisplayName is reserved and cannot be used.";
        public static final String INVALID_INITIALS = "Initials must be between 1 and 4 characters.";
        public static final String INVALID_DATE = "Date cannot be null.";
    }

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "display_name", nullable = false)
    private String displayName = "No Name";

    @Column(name = "initials", length = 4, nullable = false)
    private String initials = "AAA";

    @Column(name = "avatar_url")
    private String avatarUrl = "";

    @Column(name = "country_code", length = 10)
    private String countryCode = "";

    @Column(name = "is_profile_public", nullable = false)
    private boolean profilePublic = false;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    public UserProfile(
        UUID userId,
        String displayName,
        String initials,
        String avatarUrl,
        String countryCode,
        boolean profilePublic,
        Instant createdAtUtc
    ) {
        if (userId == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_USER_ID);
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DISPLAY_NAME);
        }

        String trimmedName = displayName.trim();
        if (RESERVED_DISPLAY_NAMES.contains(trimmedName.toLowerCase())) {
            throw new IllegalArgumentException(ErrorMessages.RESERVED_DISPLAY_NAME);
        }

        String trimmedInitials = (initials == null || initials.isBlank()) ? "AAA" : initials.trim().toUpperCase();
        if (trimmedInitials.length() < 1 || trimmedInitials.length() > 4) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_INITIALS);
        }

        this.userId = userId;
        this.displayName = trimmedName;
        this.initials = trimmedInitials;
        this.avatarUrl = avatarUrl != null ? avatarUrl.trim() : "";
        this.countryCode = countryCode != null ? countryCode.trim().toUpperCase() : "";
        this.profilePublic = profilePublic;
        this.createdAtUtc = createdAtUtc != null ? createdAtUtc : Instant.now();
    }

    public void updateProfile(
        String displayName,
        String avatarUrl,
        String countryCode,
        boolean profilePublic,
        Instant now
    ) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DISPLAY_NAME);
        }
        String trimmedName = displayName.trim();
        if (RESERVED_DISPLAY_NAMES.contains(trimmedName.toLowerCase())) {
            throw new IllegalArgumentException(ErrorMessages.RESERVED_DISPLAY_NAME);
        }
        if (now == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DATE);
        }

        this.displayName = trimmedName;
        this.avatarUrl = avatarUrl != null ? avatarUrl.trim() : "";
        this.countryCode = countryCode != null ? countryCode.trim().toUpperCase() : "";
        this.profilePublic = profilePublic;
        this.updatedAtUtc = now;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID userId;
        private String displayName;
        private String initials;
        private String avatarUrl;
        private String countryCode;
        private boolean profilePublic = false;
        private Instant createdAtUtc;
        private Instant updatedAtUtc;

        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder initials(String initials) { this.initials = initials; return this; }
        public Builder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public Builder countryCode(String countryCode) { this.countryCode = countryCode; return this; }
        public Builder profilePublic(boolean profilePublic) { this.profilePublic = profilePublic; return this; }
        public Builder createdAtUtc(Instant createdAtUtc) { this.createdAtUtc = createdAtUtc; return this; }
        public Builder updatedAtUtc(Instant updatedAtUtc) { this.updatedAtUtc = updatedAtUtc; return this; }

        public UserProfile build() {
            UserProfile profile = new UserProfile(
                userId != null ? userId : UUID.randomUUID(),
                displayName != null ? displayName : "No Name",
                initials != null ? initials : "AAA",
                avatarUrl,
                countryCode,
                profilePublic,
                createdAtUtc != null ? createdAtUtc : Instant.now()
            );
            if (updatedAtUtc != null) profile.updatedAtUtc = updatedAtUtc;
            return profile;
        }
    }
}

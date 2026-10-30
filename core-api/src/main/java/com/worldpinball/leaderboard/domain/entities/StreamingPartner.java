package com.worldpinball.leaderboard.domain.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "streaming_partners")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamingPartner {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String EMPTY_USER_ID = "UserId cannot be empty.";
        public static final String EMPTY_ORGANIZATION = "Organization name cannot be empty.";
        public static final String INVALID_CREATED_AT = "CreatedAtUtc must be a valid date.";
        public static final String REVOKED_EARLIER = "RevokedAtUtc cannot be earlier than CreatedAtUtc.";
    }

    @Id
    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "organization_name", length = 200, nullable = false)
    private String organizationName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "revoked_at_utc")
    private Instant revokedAtUtc;

    public StreamingPartner(UUID userId, String organizationName, Instant createdAtUtc) {
        if (userId == null) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_USER_ID);
        }
        if (organizationName == null || organizationName.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_ORGANIZATION);
        }
        if (createdAtUtc == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_CREATED_AT);
        }

        this.partnerId = UUID.randomUUID();
        this.userId = userId;
        this.organizationName = organizationName.trim();
        this.isActive = true;
        this.createdAtUtc = createdAtUtc;
    }

    public void revoke(Instant revokedAtUtc) {
        if (revokedAtUtc == null || revokedAtUtc.isBefore(this.createdAtUtc)) {
            throw new IllegalArgumentException(ErrorMessages.REVOKED_EARLIER);
        }
        this.isActive = false;
        this.revokedAtUtc = revokedAtUtc;
    }

    public void reactivate() {
        this.isActive = true;
        this.revokedAtUtc = null;
    }
}

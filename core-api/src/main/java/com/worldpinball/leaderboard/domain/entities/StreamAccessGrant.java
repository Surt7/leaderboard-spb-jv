package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.GrantOrigin;
import com.worldpinball.leaderboard.domain.enums.GrantStatus;
import com.worldpinball.leaderboard.domain.enums.PrincipalType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stream_access_grants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamAccessGrant {

    @Id
    @Column(name = "grant_id", nullable = false)
    private UUID grantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", length = 30, nullable = false)
    private PrincipalType principalType;

    @Column(name = "principal_id", nullable = false)
    private UUID principalId;

    @Column(name = "display_name", length = 150, nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", length = 30, nullable = false)
    private GrantOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private GrantStatus status;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "expires_at_utc")
    private Instant expiresAtUtc;

    @Column(name = "revoked_at_utc")
    private Instant revokedAtUtc;

    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;

    @Column(name = "granted_by_user_id")
    private UUID grantedByUserId;

    public StreamAccessGrant(
            PrincipalType principalType,
            UUID principalId,
            String displayName,
            GrantOrigin origin,
            Instant createdAtUtc,
            Instant expiresAtUtc,
            UUID grantedByUserId
    ) {
        if (principalId == null) {
            throw new IllegalArgumentException("PrincipalId cannot be empty.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("DisplayName cannot be empty.");
        }
        if (expiresAtUtc != null && !expiresAtUtc.isAfter(createdAtUtc)) {
            throw new IllegalArgumentException("ExpiresAtUtc must be in the future.");
        }

        this.grantId = UUID.randomUUID();
        this.principalType = principalType;
        this.principalId = principalId;
        this.displayName = displayName.trim();
        this.origin = origin;
        this.status = GrantStatus.ACTIVE;
        this.createdAtUtc = createdAtUtc != null ? createdAtUtc : Instant.now();
        this.expiresAtUtc = expiresAtUtc;
        this.grantedByUserId = grantedByUserId;
    }

    public void revoke(String reason, Instant revokedAtUtc) {
        if (this.status == GrantStatus.REVOKED) {
            throw new IllegalStateException("Grant is already revoked.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A revocation reason is required.");
        }
        this.status = GrantStatus.REVOKED;
        this.revokedAtUtc = revokedAtUtc != null ? revokedAtUtc : Instant.now();
        this.revocationReason = reason.trim();
    }

    public void reactivate() {
        this.status = GrantStatus.ACTIVE;
        this.revokedAtUtc = null;
        this.revocationReason = null;
    }

    public boolean isActive(Instant nowUtc) {
        if (this.status != GrantStatus.ACTIVE) {
            return false;
        }
        if (this.expiresAtUtc != null && !this.expiresAtUtc.isAfter(nowUtc)) {
            return false;
        }
        return true;
    }
}

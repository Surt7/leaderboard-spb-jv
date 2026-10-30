package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.RevocationReason;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "certificate_revocations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CertificateRevocation {

    @Id
    @Column(name = "revocation_id", nullable = false)
    private UUID revocationId;

    @Column(name = "device_cert_id", nullable = false, unique = true)
    private UUID deviceCertId;

    @Column(name = "revoked_at_utc", nullable = false)
    private Instant revokedAtUtc;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "reason", nullable = false)
    private RevocationReason reason;

    @Column(name = "revoked_by_user_id", nullable = false, columnDefinition = "text")
    private String revokedByUserId;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    public CertificateRevocation(
        UUID deviceCertId,
        Instant revokedAtUtc,
        RevocationReason reason,
        String revokedByUserId,
        String notes
    ) {
        this.revocationId = UUID.randomUUID();
        this.deviceCertId = Objects.requireNonNull(deviceCertId, "deviceCertId cannot be null");
        this.revokedAtUtc = Objects.requireNonNull(revokedAtUtc, "revokedAtUtc cannot be null");
        this.reason = Objects.requireNonNull(reason, "reason cannot be null");
        this.revokedByUserId = Objects.requireNonNull(revokedByUserId, "revokedByUserId cannot be null");
        this.notes = notes;
    }
}

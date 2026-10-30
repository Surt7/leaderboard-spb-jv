package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.CaType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ca_certificates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaCertificate {

    @Id
    @Column(name = "ca_id", nullable = false)
    private UUID caId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "ca_type", nullable = false)
    private CaType caType;

    @Column(name = "subject_dn", nullable = false, columnDefinition = "text")
    private String subjectDn;

    @Column(name = "issuer_dn", nullable = false, columnDefinition = "text")
    private String issuerDn;

    @Column(name = "serial_number", nullable = false, columnDefinition = "text")
    private String serialNumber;

    @Column(name = "thumbprint_sha256", nullable = false, unique = true, columnDefinition = "text")
    private String thumbprintSha256;

    @Column(name = "cert_pem", nullable = false, columnDefinition = "text")
    private String certPem;

    @Column(name = "private_key_ref", columnDefinition = "text")
    private String privateKeyRef;

    @Column(name = "not_before_utc", nullable = false)
    private Instant notBeforeUtc;

    @Column(name = "not_after_utc", nullable = false)
    private Instant notAfterUtc;

    @Column(name = "is_trusted", nullable = false)
    private boolean trusted = true;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public CaCertificate(
        CaType caType,
        String subjectDn,
        String issuerDn,
        String serialNumber,
        String thumbprintSha256,
        String certPem,
        Instant notBeforeUtc,
        Instant notAfterUtc,
        Instant createdAt,
        boolean trusted,
        boolean active,
        String privateKeyRef
    ) {
        this.caId = UUID.randomUUID();
        this.caType = Objects.requireNonNull(caType, "caType cannot be null");
        this.subjectDn = Objects.requireNonNull(subjectDn, "subjectDn cannot be null");
        this.issuerDn = Objects.requireNonNull(issuerDn, "issuerDn cannot be null");
        this.serialNumber = Objects.requireNonNull(serialNumber, "serialNumber cannot be null");
        this.thumbprintSha256 = Objects.requireNonNull(thumbprintSha256, "thumbprintSha256 cannot be null");
        this.certPem = Objects.requireNonNull(certPem, "certPem cannot be null");
        this.notBeforeUtc = Objects.requireNonNull(notBeforeUtc, "notBeforeUtc cannot be null");
        this.notAfterUtc = Objects.requireNonNull(notAfterUtc, "notAfterUtc cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.trusted = trusted;
        this.active = active;
        this.privateKeyRef = privateKeyRef;
    }

    public void setActive(boolean active, Instant now) {
        this.active = active;
        this.updatedAt = now;
    }

    public void setTrusted(boolean trusted, Instant now) {
        this.trusted = trusted;
        this.updatedAt = now;
    }
}

package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.DeviceCertificateStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "device_certificates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceCertificate {

    @Id
    @Column(name = "device_cert_id", nullable = false)
    private UUID deviceCertId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "ca_id", nullable = false)
    private UUID caId;

    @Column(name = "cert_serial_number", nullable = false, columnDefinition = "text")
    private String certSerialNumber;

    @Column(name = "cert_thumbprint_sha256", nullable = false, unique = true, columnDefinition = "text")
    private String certThumbprintSha256;

    @Column(name = "subject_dn", nullable = false, columnDefinition = "text")
    private String subjectDn;

    @Column(name = "issuer_dn", nullable = false, columnDefinition = "text")
    private String issuerDn;

    @Column(name = "cert_pem", nullable = false, columnDefinition = "text")
    private String certPem;

    @Column(name = "not_before_utc", nullable = false)
    private Instant notBeforeUtc;

    @Column(name = "not_after_utc", nullable = false)
    private Instant notAfterUtc;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private DeviceCertificateStatus status = DeviceCertificateStatus.Active;

    @Column(name = "issued_at_utc", nullable = false)
    private Instant issuedAtUtc;

    @Column(name = "revoked_at_utc")
    private Instant revokedAtUtc;

    @Column(name = "revoke_reason", columnDefinition = "text")
    private String revokeReason;

    @Column(name = "superseded_by_device_cert_id")
    private UUID supersededByDeviceCertId;

    public DeviceCertificate(
        UUID deviceId,
        UUID caId,
        String certSerialNumber,
        String certThumbprintSha256,
        String subjectDn,
        String issuerDn,
        String certPem,
        Instant notBeforeUtc,
        Instant notAfterUtc,
        Instant issuedAtUtc
    ) {
        this.deviceCertId = UUID.randomUUID();
        this.deviceId = Objects.requireNonNull(deviceId, "deviceId cannot be null");
        this.caId = Objects.requireNonNull(caId, "caId cannot be null");
        this.certSerialNumber = Objects.requireNonNull(certSerialNumber, "certSerialNumber cannot be null");
        this.certThumbprintSha256 = Objects.requireNonNull(certThumbprintSha256, "certThumbprintSha256 cannot be null");
        this.subjectDn = Objects.requireNonNull(subjectDn, "subjectDn cannot be null");
        this.issuerDn = Objects.requireNonNull(issuerDn, "issuerDn cannot be null");
        this.certPem = Objects.requireNonNull(certPem, "certPem cannot be null");
        this.notBeforeUtc = Objects.requireNonNull(notBeforeUtc, "notBeforeUtc cannot be null");
        this.notAfterUtc = Objects.requireNonNull(notAfterUtc, "notAfterUtc cannot be null");
        this.issuedAtUtc = Objects.requireNonNull(issuedAtUtc, "issuedAtUtc cannot be null");
        this.status = DeviceCertificateStatus.Active;
    }

    public void markSuperseded(UUID newCertId) {
        this.status = DeviceCertificateStatus.Superseded;
        this.supersededByDeviceCertId = newCertId;
    }

    public void markRevoked(Instant now, String reason) {
        this.status = DeviceCertificateStatus.Revoked;
        this.revokedAtUtc = now;
        this.revokeReason = reason;
    }
}

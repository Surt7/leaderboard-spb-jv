package com.worldpinball.leaderboard.application.security.ca;

import com.worldpinball.leaderboard.domain.enums.WorkstationType;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Issues X.509v3 client leaf certificates signed by an Intermediate ECDSA P-256 CA key.
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.CA.StepCaCertificateAuthorityService}.
 */
@Service
public class CertificateAuthorityService {

    private static final Logger log = LoggerFactory.getLogger(CertificateAuthorityService.class);
    private static final String ISSUER_DN = "CN=WorldPinball Intermediate CA, O=WorldPinball Internal CA";
    private static final String SOFTWARE_ISSUER_DN = "CN=WorldPinball Software Intermediate CA, O=WorldPinball Internal CA";
    private static final String DEVICE_WORKSTATION_ISSUER_DN = "CN=WorldPinball Device Intermediate CA, O=WorldPinball Internal CA";
    private static final KeyPair CA_SIGNING_KEY_PAIR = generateCaSigningKeyPair();
    private static final KeyPair SOFTWARE_CA_SIGNING_KEY_PAIR = generateCaSigningKeyPair();

    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public CertificateAuthorityService() {
        this(Clock.systemUTC());
    }

    public CertificateAuthorityService(Clock clock) {
        this.clock = clock;
    }

    public PublicKey getCaPublicKey() {
        return CA_SIGNING_KEY_PAIR.getPublic();
    }

    public IssuedCertificate issueDeviceCertificate(
            UUID machineId,
            String deviceSerialNumber,
            String publicKeyPem) {
        if (machineId == null) {
            throw new IllegalArgumentException("machineId cannot be null");
        }
        if (deviceSerialNumber == null || deviceSerialNumber.isBlank()) {
            throw new IllegalArgumentException("deviceSerialNumber cannot be null or blank");
        }
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            throw new IllegalArgumentException("publicKeyPem cannot be null or blank");
        }

        Instant now = clock.instant();
        Instant notBefore = now.minus(5, ChronoUnit.MINUTES); // Clock skew tolerance
        Instant notAfter = now.plus(365, ChronoUnit.DAYS);   // Certificate validity: 1 year

        // 1. Import device public key from PEM (ECDSA P-256 or RSA SubjectPublicKeyInfo)
        PublicKey devicePublicKey = parsePublicKeyFromPem(publicKeyPem);

        // 2. Build Subject & Issuer Distinguished Names
        String subjectDn = String.format("CN=%s, O=WorldPinball, OU=Devices", deviceSerialNumber);

        // 3. Generate random positive 16-byte serial number (MSB cleared for positive X.509 INTEGER)
        byte[] serialNumberBytes = new byte[16];
        secureRandom.nextBytes(serialNumberBytes);
        serialNumberBytes[0] &= 0x7F;
        if (serialNumberBytes[0] == 0) {
            serialNumberBytes[0] = 0x01; // Keep non-zero leading byte for 32-char hex representation
        }
        BigInteger serialBigInt = new BigInteger(1, serialNumberBytes);

        try {
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    new X500Name(ISSUER_DN),
                    serialBigInt,
                    Date.from(notBefore),
                    Date.from(notAfter),
                    new X500Name(subjectDn),
                    devicePublicKey
            );

            // 4. Configure standard X.509v3 extensions for mTLS client authentication
            // Basic Constraints: End-entity leaf certificate (cannot act as a CA), critical = true
            certBuilder.addExtension(
                    Extension.basicConstraints,
                    true,
                    new BasicConstraints(false)
            );

            // Key Usage: Digital Signature and Key Agreement, critical = true
            certBuilder.addExtension(
                    Extension.keyUsage,
                    true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement)
            );

            // Enhanced Key Usage (EKU): TLS Web Client Authentication (1.3.6.1.5.5.7.3.2), critical = false
            certBuilder.addExtension(
                    Extension.extendedKeyUsage,
                    false,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth)
            );

            // Subject Alternative Name (SAN): URN urn:pinball:machine:{machineId}
            GeneralNames subjectAltName = new GeneralNames(
                    new GeneralName(GeneralName.uniformResourceIdentifier, "urn:pinball:machine:" + machineId)
            );
            certBuilder.addExtension(
                    Extension.subjectAlternativeName,
                    false,
                    subjectAltName
            );

            // 5. Sign leaf certificate using the CA's ECDSA P-256 private signing key
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .build(CA_SIGNING_KEY_PAIR.getPrivate());
            X509CertificateHolder certHolder = certBuilder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);

            byte[] derBytes = cert.getEncoded();
            String encodedPemBody = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                    .encodeToString(derBytes);
            String certPem = "-----BEGIN CERTIFICATE-----\n" + encodedPemBody + "\n-----END CERTIFICATE-----";

            String certSerialNumber = HexFormat.of().formatHex(serialNumberBytes);
            byte[] sha256Bytes = MessageDigest.getInstance("SHA-256").digest(derBytes);
            String thumbprint = HexFormat.of().formatHex(sha256Bytes);

            log.info("Issued X.509 certificate for device serial {} (CertSerial: {})",
                    deviceSerialNumber, certSerialNumber);

            return new IssuedCertificate(
                    certPem,
                    certSerialNumber,
                    machineId,
                    thumbprint,
                    subjectDn,
                    ISSUER_DN,
                    notBefore,
                    notAfter
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue X.509v3 device certificate", e);
        }
    }

    public IssuedSoftwareCertificate issueSoftwareSigningCertificate(
            String publisherName,
            String publicKeyPem) {
        if (publisherName == null || publisherName.isBlank()) {
            throw new IllegalArgumentException("publisherName cannot be null or blank");
        }
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            throw new IllegalArgumentException("publicKeyPem cannot be null or blank");
        }

        Instant now = clock.instant();
        Instant notBefore = now.minus(5, ChronoUnit.MINUTES);
        Instant notAfter = now.plus(365, ChronoUnit.DAYS);

        PublicKey softwarePublicKey = parsePublicKeyFromPem(publicKeyPem);
        String subjectDn = String.format("CN=%s, O=WorldPinball, OU=Software", publisherName);

        byte[] serialNumberBytes = new byte[16];
        secureRandom.nextBytes(serialNumberBytes);
        serialNumberBytes[0] &= 0x7F;
        if (serialNumberBytes[0] == 0) {
            serialNumberBytes[0] = 0x01;
        }
        BigInteger serialBigInt = new BigInteger(1, serialNumberBytes);

        try {
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    new X500Name(SOFTWARE_ISSUER_DN),
                    serialBigInt,
                    Date.from(notBefore),
                    Date.from(notAfter),
                    new X500Name(subjectDn),
                    softwarePublicKey
            );

            certBuilder.addExtension(
                    Extension.basicConstraints,
                    true,
                    new BasicConstraints(false)
            );

            certBuilder.addExtension(
                    Extension.keyUsage,
                    true,
                    new KeyUsage(KeyUsage.digitalSignature)
            );

            certBuilder.addExtension(
                    Extension.extendedKeyUsage,
                    true,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_codeSigning)
            );

            GeneralNames subjectAltName = new GeneralNames(
                    new GeneralName(GeneralName.uniformResourceIdentifier, "urn:software:" + publisherName)
            );
            certBuilder.addExtension(
                    Extension.subjectAlternativeName,
                    false,
                    subjectAltName
            );

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .build(SOFTWARE_CA_SIGNING_KEY_PAIR.getPrivate());
            X509CertificateHolder certHolder = certBuilder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);

            byte[] derBytes = cert.getEncoded();
            String encodedPemBody = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                    .encodeToString(derBytes);
            String certPem = "-----BEGIN CERTIFICATE-----\n" + encodedPemBody + "\n-----END CERTIFICATE-----";

            String certSerialNumber = HexFormat.of().formatHex(serialNumberBytes);
            byte[] sha256Bytes = MessageDigest.getInstance("SHA-256").digest(derBytes);
            String thumbprint = HexFormat.of().formatHex(sha256Bytes);

            log.info("Issued X.509 certificate for software serial {} (CertSerial: {})",
                    publisherName, certSerialNumber);

            return new IssuedSoftwareCertificate(
                    certPem,
                    certSerialNumber,
                    publisherName,
                    thumbprint,
                    subjectDn,
                    SOFTWARE_ISSUER_DN,
                    notBefore,
                    notAfter
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue X.509v3 software signing certificate", e);
        }
    }

    public IssuedWorkstationCertificate issueWorkstationCertificate(
            String workstationName,
            String publicKeyPem,
            WorkstationType workstationType) {
        if (workstationName == null || workstationName.isBlank()) {
            throw new IllegalArgumentException("workstationName cannot be null or blank");
        }
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            throw new IllegalArgumentException("publicKeyPem cannot be null or blank");
        }
        WorkstationType effectiveType = workstationType != null ? workstationType : WorkstationType.AdminWorkstation;

        Instant now = clock.instant();
        Instant notBefore = now.minus(5, ChronoUnit.MINUTES);
        Instant notAfter = now.plus(365, ChronoUnit.DAYS);

        PublicKey workstationPublicKey = parsePublicKeyFromPem(publicKeyPem);

        String ou = effectiveType == WorkstationType.AdminWorkstation ? "AdminWorkstations" : "FieldTerminals";
        String urnPrefix = effectiveType == WorkstationType.AdminWorkstation ? "admin" : "terminal";
        String subjectDn = String.format("CN=%s, O=WorldPinball, OU=%s", workstationName, ou);

        byte[] serialNumberBytes = new byte[16];
        secureRandom.nextBytes(serialNumberBytes);
        serialNumberBytes[0] &= 0x7F;
        if (serialNumberBytes[0] == 0) {
            serialNumberBytes[0] = 0x01;
        }
        BigInteger serialBigInt = new BigInteger(1, serialNumberBytes);

        try {
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    new X500Name(DEVICE_WORKSTATION_ISSUER_DN),
                    serialBigInt,
                    Date.from(notBefore),
                    Date.from(notAfter),
                    new X500Name(subjectDn),
                    workstationPublicKey
            );

            certBuilder.addExtension(
                    Extension.basicConstraints,
                    true,
                    new BasicConstraints(false)
            );

            certBuilder.addExtension(
                    Extension.keyUsage,
                    true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement)
            );

            certBuilder.addExtension(
                    Extension.extendedKeyUsage,
                    true,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth)
            );

            GeneralNames subjectAltName = new GeneralNames(
                    new GeneralName(GeneralName.uniformResourceIdentifier,
                            "urn:pinball:workstation:" + urnPrefix + ":" + workstationName)
            );
            certBuilder.addExtension(
                    Extension.subjectAlternativeName,
                    false,
                    subjectAltName
            );

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .build(CA_SIGNING_KEY_PAIR.getPrivate());
            X509CertificateHolder certHolder = certBuilder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);

            byte[] derBytes = cert.getEncoded();
            String encodedPemBody = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                    .encodeToString(derBytes);
            String certPem = "-----BEGIN CERTIFICATE-----\n" + encodedPemBody + "\n-----END CERTIFICATE-----";

            String certSerialNumber = HexFormat.of().formatHex(serialNumberBytes);
            byte[] sha256Bytes = MessageDigest.getInstance("SHA-256").digest(derBytes);
            String thumbprint = HexFormat.of().formatHex(sha256Bytes);

            log.info("Issued X.509 workstation client certificate for {} (Serial: {})",
                    workstationName, certSerialNumber);

            return new IssuedWorkstationCertificate(
                    certPem,
                    certSerialNumber,
                    workstationName,
                    thumbprint,
                    subjectDn,
                    DEVICE_WORKSTATION_ISSUER_DN,
                    notBefore,
                    notAfter
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue X.509v3 workstation certificate", e);
        }
    }

    public static PublicKey parsePublicKeyFromPem(String publicKeyPem) {
        String normalized = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 in public key PEM", e);
        }

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        try {
            return KeyFactory.getInstance("EC").generatePublic(keySpec);
        } catch (GeneralSecurityException ecEx) {
            throw new IllegalArgumentException("Unsupported or malformed ECDSA SubjectPublicKeyInfo PEM", ecEx);
        }
    }

    private static KeyPair generateCaSigningKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to initialize ECDSA P-256 CA signing key", e);
        }
    }
}

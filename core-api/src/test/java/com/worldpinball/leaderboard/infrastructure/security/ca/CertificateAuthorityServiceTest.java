package com.worldpinball.leaderboard.infrastructure.security.ca;

import com.worldpinball.leaderboard.application.security.ca.CertificateAuthorityService;
import com.worldpinball.leaderboard.application.security.ca.IssuedCertificate;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CertificateAuthorityService X.509v3 ECDSA Issuance Tests")
class CertificateAuthorityServiceTest {

    private CertificateAuthorityService caService;
    private String clientPublicKeyPem;

    @BeforeEach
    void setUp() throws Exception {
        caService = new CertificateAuthorityService();

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = keyGen.generateKeyPair();
        clientPublicKeyPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(kp.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }

    @Test
    @DisplayName("IssueDeviceCertificate produces valid X.509v3 cert with SAN URI urn:pinball:machine:{machineId}")
    void issueDeviceCertificate_producesValidX509v3Certificate() throws Exception {
        UUID machineId = UUID.randomUUID();
        String deviceSerial = "WP-DEV-TEST-001";

        IssuedCertificate issued = caService.issueDeviceCertificate(machineId, deviceSerial, clientPublicKeyPem);

        assertThat(issued).isNotNull();
        assertThat(issued.machineId()).isEqualTo(machineId);
        assertThat(issued.serialNumber()).isNotBlank();
        assertThat(issued.thumbprintSha256()).isNotBlank().hasSize(64);
        assertThat(issued.subjectDn()).contains("CN=" + deviceSerial);
        assertThat(issued.issuerDn()).contains("CN=WorldPinball Intermediate CA");
        assertThat(issued.notBeforeUtc()).isNotNull();
        assertThat(issued.notAfterUtc()).isAfter(issued.notBeforeUtc());
        assertThat(issued.certPem()).contains("-----BEGIN CERTIFICATE-----");

        try (PEMParser pemParser = new PEMParser(new StringReader(issued.certPem()))) {
            Object parsed = pemParser.readObject();
            assertThat(parsed).isInstanceOf(X509CertificateHolder.class);
            X509CertificateHolder holder = (X509CertificateHolder) parsed;

            Extension sanExt = holder.getExtension(Extension.subjectAlternativeName);
            assertThat(sanExt).isNotNull();

            GeneralNames gns = GeneralNames.getInstance(sanExt.getParsedValue());
            boolean hasExpectedSanUri = false;
            for (GeneralName gn : gns.getNames()) {
                if (gn.getTagNo() == GeneralName.uniformResourceIdentifier) {
                    if (gn.getName().toString().equals("urn:pinball:machine:" + machineId)) {
                        hasExpectedSanUri = true;
                    }
                }
            }
            assertThat(hasExpectedSanUri).isTrue();
        }
    }

    @Test
    @DisplayName("IssueDeviceCertificate rejects invalid public key")
    void issueDeviceCertificate_withInvalidPublicKey_throwsIllegalArgumentException() {
        UUID machineId = UUID.randomUUID();
        assertThatThrownBy(() -> caService.issueDeviceCertificate(machineId, "WP-DEV-001", "invalid-pem"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

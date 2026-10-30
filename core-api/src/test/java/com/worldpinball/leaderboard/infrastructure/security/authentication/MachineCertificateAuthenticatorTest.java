package com.worldpinball.leaderboard.infrastructure.security.authentication;

import com.worldpinball.leaderboard.application.security.ca.CertificateAuthorityService;
import com.worldpinball.leaderboard.application.security.ca.IssuedCertificate;
import com.worldpinball.leaderboard.infrastructure.security.authentication.MachineCertificateAuthenticator.CertificateAuthResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MachineCertificateAuthenticator Tests")
class MachineCertificateAuthenticatorTest {

    @Test
    @DisplayName("Extracts machineId from SAN URN and attaches Machine role")
    void authenticate_whenValidLeafCert_returnsSuccessPrincipal() throws Exception {
        UUID machineId = UUID.randomUUID();
        CertificateAuthorityService caService = new CertificateAuthorityService();

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = keyGen.generateKeyPair();
        String pubPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(kp.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";

        IssuedCertificate issued = caService.issueDeviceCertificate(machineId, "DEV-SN-99", pubPem);
        X509Certificate x509 = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(issued.certPem().getBytes(StandardCharsets.UTF_8)));

        MachineCertificateAuthenticator sut = new MachineCertificateAuthenticator();
        CertificateAuthResult result = sut.authenticate(x509);

        assertThat(result).isInstanceOf(CertificateAuthResult.Success.class);
        CertificateAuthResult.Success success = (CertificateAuthResult.Success) result;
        assertThat(success.machineId()).isEqualTo(machineId);
        assertThat(success.principal().findFirst(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER))
                .contains(machineId.toString());
        assertThat(success.principal().isInRole("Machine")).isTrue();
    }

    @Test
    @DisplayName("Rejects null certificate with Failure result")
    void authenticate_whenNullCert_returnsFailure() {
        MachineCertificateAuthenticator sut = new MachineCertificateAuthenticator();
        CertificateAuthResult result = sut.authenticate(null);

        assertThat(result).isInstanceOf(CertificateAuthResult.Failure.class);
    }
}

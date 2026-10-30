package com.worldpinball.leaderboard.infrastructure.security.authentication;

import com.worldpinball.leaderboard.application.security.ca.CertificateAuthorityService;
import com.worldpinball.leaderboard.application.security.ca.IssuedCertificate;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("ClientCertificateForwardingFilter Tests")
class ClientCertificateForwardingFilterTest {

    @Test
    @DisplayName("Extracts and decodes X-Client-Cert into jakarta.servlet.request.X509Certificate attribute")
    void doFilter_withValidUrlEncodedCert_setsServletAttribute() throws Exception {
        UUID machineId = UUID.randomUUID();
        CertificateAuthorityService caService = new CertificateAuthorityService();

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = keyGen.generateKeyPair();
        String pubPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(kp.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";

        IssuedCertificate issued = caService.issueDeviceCertificate(machineId, "DEV-SN-123", pubPem);
        String encodedPem = URLEncoder.encode(issued.certPem(), StandardCharsets.UTF_8);

        ClientCertificateForwardingFilter filter = new ClientCertificateForwardingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ClientCertificateForwardingFilter.X_CLIENT_CERT_HEADER, encodedPem);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);

        Object attr = request.getAttribute(ClientCertificateForwardingFilter.SERVLET_CERT_ATTRIBUTE);
        assertThat(attr).isNotNull().isInstanceOf(X509Certificate[].class);
        X509Certificate[] certs = (X509Certificate[]) attr;
        assertThat(certs).hasSize(1);
        assertThat(certs[0].getSubjectX500Principal().getName()).contains("DEV-SN-123");
    }

    @Test
    @DisplayName("Does nothing when X-Client-Cert header is absent")
    void doFilter_withoutHeader_passesThroughWithoutSettingAttribute() throws Exception {
        ClientCertificateForwardingFilter filter = new ClientCertificateForwardingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(request.getAttribute(ClientCertificateForwardingFilter.SERVLET_CERT_ATTRIBUTE)).isNull();
    }
}

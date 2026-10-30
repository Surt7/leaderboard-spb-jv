package com.worldpinball.leaderboard.infrastructure.security.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Filter that parses forwarded client certificates from Nginx (via the {@code X-Client-Cert} header)
 * and attaches them to the {@code jakarta.servlet.request.X509Certificate} request attribute.
 * Mirror of ASP.NET's {@code AddCertificateForwarding} middleware.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@Slf4j
public class ClientCertificateForwardingFilter extends OncePerRequestFilter {

    public static final String X_CLIENT_CERT_HEADER = "X-Client-Cert";
    public static final String SERVLET_CERT_ATTRIBUTE = MachineCertificateAuthenticator.SERVLET_CERT_ATTRIBUTE;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String certHeader = request.getHeader(X_CLIENT_CERT_HEADER);
        if (certHeader != null && !certHeader.isBlank()) {
            try {
                String decodedPem = URLDecoder.decode(certHeader, StandardCharsets.UTF_8);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                try (ByteArrayInputStream bais = new ByteArrayInputStream(decodedPem.getBytes(StandardCharsets.UTF_8))) {
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(bais);
                    if (cert != null) {
                        request.setAttribute(SERVLET_CERT_ATTRIBUTE, new X509Certificate[]{cert});
                        log.debug("Successfully forwarded client certificate for subject: {}", cert.getSubjectX500Principal());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to decode or parse forwarded client certificate from {}: {}", X_CLIENT_CERT_HEADER, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}

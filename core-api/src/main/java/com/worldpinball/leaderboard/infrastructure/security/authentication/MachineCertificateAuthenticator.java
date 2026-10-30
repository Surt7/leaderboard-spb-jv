package com.worldpinball.leaderboard.infrastructure.security.authentication;

import com.worldpinball.leaderboard.infrastructure.security.authentication.ClaimsPrincipal.Claim;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Validates mTLS client X.509 certificates presented by physical pinball machines
 * (extracted by Tomcat via {@code jakarta.servlet.request.X509Certificate})
 * and extracts the machine identity from the SAN URI {@code urn:pinball:machine:{machineId}}.
 * Mirrors C# {@code SecurityExtensions.ValidateMachineCertificateAsync}.
 */
@Component
public class MachineCertificateAuthenticator {

    public static final String SCHEME_NAME = "Certificate";
    public static final String SERVLET_CERT_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";
    private static final String EXPECTED_URN_PREFIX = "pinball:machine:";
    private static final int SAN_TYPE_URI = 6;

    public sealed interface CertificateAuthResult {
        record Success(ClaimsPrincipal principal, UUID machineId) implements CertificateAuthResult {}
        record Failure(String reason) implements CertificateAuthResult {}
    }

    public CertificateAuthResult authenticate(X509Certificate certificate) {
        if (certificate == null) {
            return new CertificateAuthResult.Failure("Client certificate is missing.");
        }

        // 1. Extract URI from certificate SAN
        String uriString = extractFirstSanUri(certificate);
        if (uriString == null || uriString.isBlank()) {
            return new CertificateAuthResult.Failure("Certificate does not contain a SAN URI.");
        }

        // 2. Validate absolute URI format
        URI uri;
        try {
            uri = new URI(uriString);
            if (!uri.isAbsolute()) {
                return new CertificateAuthResult.Failure("Certificate does not contain a valid absolute URI.");
            }
        } catch (URISyntaxException e) {
            return new CertificateAuthResult.Failure("Certificate does not contain a valid absolute URI.");
        }

        // 3. Ensure the URI scheme is URN
        if (!"urn".equalsIgnoreCase(uri.getScheme())) {
            return new CertificateAuthResult.Failure("Certificate URI scheme is not URN.");
        }

        // 4. Ensure it belongs to the pinball machine namespace
        String schemeSpecificPart = uri.getSchemeSpecificPart();
        if (schemeSpecificPart == null
                || !schemeSpecificPart.toLowerCase().startsWith(EXPECTED_URN_PREFIX)) {
            return new CertificateAuthResult.Failure("Certificate URN does not belong to pinball:machine namespace.");
        }

        // 5. Extract and parse the machine UUID
        String rawUuid = schemeSpecificPart.substring(EXPECTED_URN_PREFIX.length());
        UUID machineId;
        try {
            machineId = UUID.fromString(rawUuid);
        } catch (IllegalArgumentException e) {
            return new CertificateAuthResult.Failure("Certificate URN does not contain a valid machine GUID.");
        }

        // 6. Attach the Machine claims identity
        ClaimsPrincipal principal = new ClaimsPrincipal(
                SCHEME_NAME,
                List.of(
                        new Claim(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER, machineId.toString()),
                        new Claim(ClaimsPrincipal.CLAIM_ROLE, "Machine")
                )
        );

        return new CertificateAuthResult.Success(principal, machineId);
    }

    private String extractFirstSanUri(X509Certificate cert) {
        try {
            Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans == null) {
                return null;
            }
            for (List<?> entry : sans) {
                if (entry != null && entry.size() >= 2
                        && entry.get(0) instanceof Integer type && type == SAN_TYPE_URI
                        && entry.get(1) instanceof String uriVal) {
                    return uriVal;
                }
            }
            return null;
        } catch (CertificateParsingException e) {
            return null;
        }
    }
}

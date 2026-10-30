package com.worldpinball.leaderboard.infrastructure.security.cryptography;

import com.worldpinball.leaderboard.application.security.ca.CertificateAuthorityService;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * Cryptographic signature verifier using ECDSA over NIST P-256 (SHA-256).
 * Automatically detects and supports both raw IEEE P1363 (64-byte {@code r||s} concatenation
 * typical of hardware TPM/microcontrollers via {@code SHA256withECDSAinP1363Format})
 * and RFC 3279 ASN.1 DER sequence formats ({@code SHA256withECDSA}).
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.Cryptography.EcdsaP256SignatureVerifier}.
 */
@Component
public class EcdsaP256SignatureVerifier implements SignatureVerifier {

    private static final String ALLOWED_ALG = "ecdsa-p256-sha256";

    @Override
    public boolean verify(String publicKeyPem, byte[] data, String signatureBase64, String algorithm) {
        if (algorithm != null && !algorithm.isBlank() && !ALLOWED_ALG.equalsIgnoreCase(algorithm.trim())) {
            throw new SecurityException("Algorithm not supported: " + algorithm);
        }
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            throw new IllegalArgumentException("Public key PEM is missing.");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data is missing.");
        }
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            throw new IllegalArgumentException("Signature is missing.");
        }

        byte[] sigBytes;
        try {
            sigBytes = Base64.getDecoder().decode(signatureBase64);
        } catch (IllegalArgumentException ex) {
            throw new SecurityException("Invalid X-Signature (base64).", ex);
        }

        // 64 bytes -> IEEE P1363 fixed-field concatenation (r || s); otherwise ASN.1 DER sequence
        String jcaAlgorithm = (sigBytes.length == 64)
                ? "SHA256withECDSAinP1363Format"
                : "SHA256withECDSA";

        try {
            PublicKey publicKey = CertificateAuthorityService.parsePublicKeyFromPem(publicKeyPem);
            Signature verifier = Signature.getInstance(jcaAlgorithm);
            verifier.initVerify(publicKey);
            verifier.update(data);
            return verifier.verify(sigBytes);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new SecurityException("Signature verification failed: " + ex.getMessage(), ex);
        }
    }
}

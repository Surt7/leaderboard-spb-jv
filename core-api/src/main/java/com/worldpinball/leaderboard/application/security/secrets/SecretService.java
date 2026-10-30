package com.worldpinball.leaderboard.application.security.secrets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Cryptographic service for generating, hashing (SHA-256), and protecting (AES-256-GCM)
 * 256-bit machine enrollment secrets.
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.Secrets.SecretService}.
 */
@Service
public class SecretService {

    private static final int SECRET_SIZE_BYTES = 32;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final byte[] PURPOSE_AAD =
            "WorldPinball.MachineSecret.v1".getBytes(StandardCharsets.UTF_8);

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey masterKey;

    public SecretService() {
        this(generateEphemeralKey());
    }

    @Autowired
    public SecretService(@Value("${security.secrets.master-key-base64:}") String masterKeyBase64) {
        if (masterKeyBase64 != null && !masterKeyBase64.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(masterKeyBase64.trim());
            if (decoded.length != 32) {
                throw new IllegalArgumentException("Master key must be exactly 32 bytes (256 bits).");
            }
            this.masterKey = new SecretKeySpec(decoded, "AES");
        } else {
            this.masterKey = generateEphemeralKey();
        }
    }

    public SecretService(SecretKey masterKey) {
        this.masterKey = masterKey;
    }

    public SecretGenerationResult generate() {
        byte[] secretBytes = new byte[SECRET_SIZE_BYTES];
        secureRandom.nextBytes(secretBytes);

        String secretBase64 = Base64.getEncoder().encodeToString(secretBytes);
        String hashHex = sha256Hex(secretBytes);
        String encrypted = protect(secretBase64);

        return new SecretGenerationResult(secretBase64, hashHex, encrypted);
    }

    public Optional<String> tryHash(String plainBase64Secret) {
        if (plainBase64Secret == null || plainBase64Secret.isBlank()) {
            return Optional.empty();
        }

        try {
            byte[] secretBytes = Base64.getDecoder().decode(plainBase64Secret);
            if (secretBytes.length != SECRET_SIZE_BYTES) {
                return Optional.empty();
            }
            return Optional.of(sha256Hex(secretBytes));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String protect(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("plainText cannot be null or blank");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            cipher.updateAAD(PURPOSE_AAD);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer payload = ByteBuffer.allocate(iv.length + cipherText.length);
            payload.put(iv);
            payload.put(cipherText);

            return Base64.getEncoder().encodeToString(payload.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-256-GCM encryption failed", e);
        }
    }

    public String unprotect(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            throw new IllegalArgumentException("encryptedBase64 cannot be null or blank");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedBase64);
            if (payload.length <= GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
                throw new IllegalArgumentException("Invalid AES-GCM payload length");
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload, 0, GCM_IV_LENGTH_BYTES);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);
            cipher.updateAAD(PURPOSE_AAD);

            byte[] plainBytes = cipher.doFinal(payload, GCM_IV_LENGTH_BYTES, payload.length - GCM_IV_LENGTH_BYTES);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new SecurityException("AES-256-GCM decryption or authentication tag verification failed", e);
        }
    }

    private String sha256Hex(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static SecretKey generateEphemeralKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("AES KeyGenerator not available", e);
        }
    }
}

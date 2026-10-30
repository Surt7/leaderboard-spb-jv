package com.worldpinball.leaderboard.infrastructure.security.secrets;

import com.worldpinball.leaderboard.application.security.secrets.SecretGenerationResult;
import com.worldpinball.leaderboard.application.security.secrets.SecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SecretService AES-256-GCM and SHA-256 Tests")
class SecretServiceTest {

    private SecretService secretService;

    @BeforeEach
    void setUp() {
        secretService = new SecretService();
    }

    @Test
    @DisplayName("Generate produces a valid 32-byte Base64 secret, its 64-char lowercase hex SHA-256 and encrypted payload")
    void generate_producesValidSecretResult() {
        SecretGenerationResult result = secretService.generate();

        assertThat(result).isNotNull();
        assertThat(result.plainBase64()).isNotBlank();
        byte[] decoded = Base64.getDecoder().decode(result.plainBase64());
        assertThat(decoded).hasSize(32);

        assertThat(result.hashHex())
                .isNotBlank()
                .hasSize(64)
                .matches("^[0-9a-f]{64}$");

        assertThat(result.encrypted()).isNotBlank();
    }

    @Test
    @DisplayName("TryHash returns matching lowercase hex for a freshly generated secret")
    void tryHash_withGeneratedSecret_matchesHashHex() {
        SecretGenerationResult result = secretService.generate();

        Optional<String> computedHash = secretService.tryHash(result.plainBase64());

        assertThat(computedHash).isPresent();
        assertThat(computedHash.get()).isEqualTo(result.hashHex());
    }

    @Test
    @DisplayName("Protect and unprotect roundtrip returns the original secret")
    void protectAndUnprotect_roundtripPreservesPlainSecret() {
        SecretGenerationResult result = secretService.generate();

        String encrypted = secretService.protect(result.plainBase64());
        assertThat(encrypted).isNotBlank().isNotEqualTo(result.plainBase64());

        String decrypted = secretService.unprotect(encrypted);
        assertThat(decrypted).isEqualTo(result.plainBase64());
    }

    @Test
    @DisplayName("Two consecutive calls to protect with same plaintext yield different ciphertexts (random IV)")
    void protect_usesRandomIv_producesDifferentCiphertexts() {
        String plaintext = "my-test-secret-value-1234567890";

        String ciphertext1 = secretService.protect(plaintext);
        String ciphertext2 = secretService.protect(plaintext);

        assertThat(ciphertext1).isNotEqualTo(ciphertext2);
        assertThat(secretService.unprotect(ciphertext1)).isEqualTo(plaintext);
        assertThat(secretService.unprotect(ciphertext2)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("Unprotect with tampered ciphertext fails with SecurityException")
    void unprotect_withTamperedCiphertext_throwsSecurityException() {
        String encrypted = secretService.protect("super-secret-key");
        byte[] raw = Base64.getDecoder().decode(encrypted);
        raw[raw.length - 1] ^= 0xFF; // flip last byte of tag
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> secretService.unprotect(tampered))
                .isInstanceOf(SecurityException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("TryHash returns empty for null or whitespace secret")
    void tryHash_withNullOrWhitespace_returnsEmpty(String invalidSecret) {
        assertThat(secretService.tryHash(invalidSecret)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-base-64!!",
            "c2hvcnQ=", // "short" (5 bytes)
            "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEy" // 33 bytes
    })
    @DisplayName("TryHash returns empty when secret is not valid 32-byte Base64")
    void tryHash_withInvalidLengthOrNonBase64_returnsEmpty(String invalidSecret) {
        assertThat(secretService.tryHash(invalidSecret)).isEmpty();
    }

    @Test
    @DisplayName("Unprotect throws IllegalArgumentException on empty or null ciphertext")
    void unprotect_withInvalidInput_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> secretService.unprotect(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> secretService.unprotect("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Protect throws IllegalArgumentException on empty or null plaintext")
    void protect_withInvalidInput_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> secretService.protect(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> secretService.protect("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

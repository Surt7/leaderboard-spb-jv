package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.EnrollmentTokenType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EnrollmentToken Domain Entity Unit Tests")
class EnrollmentTokenTest {

    private static final Instant NOW_UTC = Instant.parse("2026-09-10T12:00:00Z");

    @Test
    void constructor_withValidArguments_shouldInitializeProperties() {
        String tokenHash = "dummy_hash_sha256_value";
        EnrollmentTokenType tokenType = EnrollmentTokenType.DeviceEnroll;
        String issuedByUserId = "admin-user-id";
        Instant issuedAt = NOW_UTC;
        Instant expiresAt = NOW_UTC.plus(7, ChronoUnit.DAYS);
        UUID machineId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        int maxUses = 3;
        String notes = "Test token notes";

        EnrollmentToken token = new EnrollmentToken(
                tokenHash,
                tokenType,
                issuedByUserId,
                issuedAt,
                expiresAt,
                machineId,
                deviceId,
                maxUses,
                notes,
                null
        );

        assertThat(token.getTokenId()).isNotNull();
        assertThat(token.getTokenHash()).isEqualTo(tokenHash);
        assertThat(token.getTokenType()).isEqualTo(tokenType);
        assertThat(token.getIssuedByUserId()).isEqualTo(issuedByUserId);
        assertThat(token.getIssuedAtUtc()).isEqualTo(issuedAt);
        assertThat(token.getExpiresAtUtc()).isEqualTo(expiresAt);
        assertThat(token.getMachineId()).isEqualTo(machineId);
        assertThat(token.getDeviceId()).isEqualTo(deviceId);
        assertThat(token.getMaxUses()).isEqualTo(maxUses);
        assertThat(token.getNotes()).isEqualTo(notes);
        assertThat(token.getValidityHours()).isEqualTo(EnrollmentToken.DEFAULT_VALIDITY_HOURS);
        assertThat(token.getUsesCount()).isEqualTo(0);
        assertThat(token.getConsumedAtUtc()).isNull();
        assertThat(token.getConsumedByDeviceId()).isNull();
        assertThat(token.getRevokedAt()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void constructor_whenTokenHashIsNullOrEmpty_throwsIllegalArgumentException(String invalidHash) {
        assertThatThrownBy(() -> new EnrollmentToken(
                invalidHash,
                EnrollmentTokenType.DeviceEnroll,
                "admin-user-id",
                NOW_UTC,
                NOW_UTC.plus(7, ChronoUnit.DAYS),
                UUID.randomUUID(),
                UUID.randomUUID(),
                3,
                "Notes",
                null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage(EnrollmentToken.ErrorMessages.TOKEN_HASH);
    }

    @Test
    void constructor_whenValidityHoursIsCustom_shouldSetValidityHours() {
        EnrollmentToken token = new EnrollmentToken(
                "dummy_hash_sha256_value",
                EnrollmentTokenType.DeviceEnroll,
                "admin-user-id",
                NOW_UTC,
                NOW_UTC.plus(48, ChronoUnit.HOURS),
                null,
                null,
                1,
                null,
                48
        );

        assertThat(token.getValidityHours()).isEqualTo(48);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5})
    void constructor_whenValidityHoursIsZeroOrNegative_shouldThrowIllegalArgumentException(int invalidHours) {
        assertThatThrownBy(() -> new EnrollmentToken(
                "dummy_hash_sha256_value",
                EnrollmentTokenType.DeviceEnroll,
                "admin-user-id",
                NOW_UTC,
                NOW_UTC.plus(24, ChronoUnit.HOURS),
                null,
                null,
                1,
                null,
                invalidHours
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage(EnrollmentToken.ErrorMessages.INVALID_VALIDITY_HOURS);
    }

    @Test
    void constructor_whenExpiresAtIsBeforeOrEqualIssuedAt_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> new EnrollmentToken(
                "dummy_hash_sha256_value",
                EnrollmentTokenType.DeviceEnroll,
                "admin-user-id",
                NOW_UTC,
                NOW_UTC.minusSeconds(600),
                null,
                null,
                1,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage(EnrollmentToken.ErrorMessages.EXPIRES_AT);
    }

    @Test
    void consume_withValidDevice_shouldSetConsumedAt() {
        UUID expectedValidDeviceId = UUID.randomUUID();
        EnrollmentToken token = new EnrollmentToken(
                "dummy_hash_sha256_value",
                EnrollmentTokenType.DeviceEnroll,
                "admin-user-id",
                NOW_UTC,
                NOW_UTC.plus(7, ChronoUnit.DAYS),
                UUID.randomUUID(),
                expectedValidDeviceId,
                1,
                "Notes",
                null
        );

        Instant consumeDate = NOW_UTC.plus(1, ChronoUnit.HOURS);
        token.consume(expectedValidDeviceId, consumeDate);

        assertThat(token.getUsesCount()).isEqualTo(1);
        assertThat(token.getConsumedAtUtc()).isEqualTo(consumeDate);
        assertThat(token.getConsumedByDeviceId()).isEqualTo(expectedValidDeviceId);
    }

    @Test
    void consume_whenDeviceIdIsNull_shouldThrowIllegalArgumentException() {
        EnrollmentToken token = new EnrollmentToken(
                "dummy_hash_sha256_value",
                EnrollmentTokenType.DeviceEnroll,
                "admin-user-id",
                NOW_UTC,
                NOW_UTC.plus(7, ChronoUnit.DAYS),
                null, null, 1, null, null
        );

        assertThatThrownBy(() -> token.consume(null, NOW_UTC.plus(1, ChronoUnit.HOURS)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(EnrollmentToken.ErrorMessages.INVALID_DEVICE_ID);
    }

    @Test
    void consume_whenNowIsNull_shouldThrowIllegalArgumentException() {
        EnrollmentToken token = new EnrollmentToken(
                "dummy_hash_sha256_value",
                EnrollmentTokenType.DeviceEnroll,
                "admin-user-id",
                NOW_UTC,
                NOW_UTC.plus(7, ChronoUnit.DAYS),
                null, null, 1, null, null
        );

        assertThatThrownBy(() -> token.consume(UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(EnrollmentToken.ErrorMessages.INVALID_DATE);
    }

    @Test
    void consume_whenTokenIsExpired_shouldThrowIllegalStateException() {
        UUID deviceId = UUID.randomUUID();
        EnrollmentToken token = new EnrollmentToken(
                "dummy_hash_sha256_value",
                EnrollmentTokenType.DeviceEnroll,
                "admin-user-id",
                NOW_UTC,
                NOW_UTC.plus(5, ChronoUnit.DAYS),
                UUID.randomUUID(),
                deviceId,
                1,
                "Notes",
                null
        );

        Instant expiredDate = NOW_UTC.plus(8, ChronoUnit.DAYS);

        assertThatThrownBy(() -> token.consume(deviceId, expiredDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(EnrollmentToken.ErrorMessages.EXPIRED_AT);
    }

    @Test
    void consume_whenTokenIsScopedToDifferentDevice_shouldThrowIllegalStateException() {
        UUID scopedDeviceId = UUID.randomUUID();
        EnrollmentToken token = new EnrollmentToken(
                "dummy_hash_sha256_value",
                EnrollmentTokenType.DeviceEnroll,
                "admin-user-id",
                NOW_UTC,
                NOW_UTC.plus(5, ChronoUnit.DAYS),
                UUID.randomUUID(),
                scopedDeviceId,
                1,
                "Notes",
                null
        );

        UUID attackerDeviceId = UUID.randomUUID();

        assertThatThrownBy(() -> token.consume(attackerDeviceId, NOW_UTC.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(EnrollmentToken.ErrorMessages.DIFFERENT_DEVICE);
    }

    @Test
    void consume_whenTokenIsUnscoped_shouldAllowAnyDevice() {
        EnrollmentToken token = new EnrollmentToken(
                "dummy_hash_sha256_value",
                EnrollmentTokenType.DeviceEnroll,
                "admin-user-id",
                NOW_UTC,
                NOW_UTC.plus(5, ChronoUnit.DAYS),
                UUID.randomUUID(),
                null,
                1,
                "Notes",
                null
        );

        UUID anyDeviceId = UUID.randomUUID();
        token.consume(anyDeviceId, NOW_UTC.plus(1, ChronoUnit.HOURS));

        assertThat(token.getUsesCount()).isEqualTo(1);
        assertThat(token.getConsumedByDeviceId()).isEqualTo(anyDeviceId);
    }

    @Test
    void revoke_whenCalled_shouldSetRevokedAt() {
        EnrollmentToken token = new EnrollmentToken(
                "dummy_hash_sha256_value",
                EnrollmentTokenType.DeviceEnroll,
                "admin-user-id",
                NOW_UTC,
                NOW_UTC.plus(7, ChronoUnit.DAYS),
                null, null, 1, null, null
        );

        Instant revokeDate = NOW_UTC.plus(2, ChronoUnit.HOURS);
        token.revoke(revokeDate);

        assertThat(token.getRevokedAt()).isEqualTo(revokeDate);
    }

    @Test
    void revoke_whenNowIsNull_shouldThrowIllegalArgumentException() {
        EnrollmentToken token = new EnrollmentToken(
                "dummy_hash_sha256_value",
                EnrollmentTokenType.DeviceEnroll,
                "admin-user-id",
                NOW_UTC,
                NOW_UTC.plus(7, ChronoUnit.DAYS),
                null, null, 1, null, null
        );

        assertThatThrownBy(() -> token.revoke(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(EnrollmentToken.ErrorMessages.INVALID_DATE);
    }
}

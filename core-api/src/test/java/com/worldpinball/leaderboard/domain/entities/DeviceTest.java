package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.DeviceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Device Domain Entity Unit Tests")
class DeviceTest {

    private static final Instant NOW_UTC = Instant.parse("2026-09-24T12:00:00Z");
    private static final String VALID_SERIAL = "DEV-PHYS-001";
    private static final String VALID_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE\n-----END PUBLIC KEY-----";
    private static final String VALID_ALGORITHM = "ES256";

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void constructor_whenSerialPhysicalIsEmptyOrNull_shouldThrowIllegalArgumentException(String invalidSerial) {
        assertThatThrownBy(() -> new Device(invalidSerial, NOW_UTC, UUID.randomUUID(), "1.0.0", "notes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(Device.ErrorMessages.EMPTY_SERIAL_PHYSICAL);
    }

    @Test
    void constructor_withValidArguments_shouldInitializeInPendingStatus() {
        Device device = new Device(VALID_SERIAL, NOW_UTC, UUID.randomUUID(), "1.0.0", "Initial device");

        assertThat(device.getDeviceId()).isNotNull();
        assertThat(device.getSerialPhysical()).isEqualTo(VALID_SERIAL);
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.PENDING);
        assertThat(device.getCreatedAtUtc()).isEqualTo(NOW_UTC);
        assertThat(device.getClientVersion()).isEqualTo("1.0.0");
        assertThat(device.getNotes()).isEqualTo("Initial device");
        assertThat(device.getLastCounter()).isEqualTo(0L);
        assertThat(device.getActivatedAtUtc()).isNull();
        assertThat(device.getRevokedAtUtc()).isNull();
    }

    @Test
    void activate_whenDeviceIsPending_shouldTransitionToActive() {
        Device device = new Device(VALID_SERIAL, NOW_UTC, null, null, null);
        Instant activatedAt = NOW_UTC.plusSeconds(300);

        device.activate(VALID_PUBLIC_KEY, VALID_ALGORITHM, activatedAt);

        assertThat(device.getStatus()).isEqualTo(DeviceStatus.ACTIVE);
        assertThat(device.getPublicKey()).isEqualTo(VALID_PUBLIC_KEY);
        assertThat(device.getKeyAlgorithm()).isEqualTo(VALID_ALGORITHM);
        assertThat(device.getActivatedAtUtc()).isEqualTo(activatedAt);
        assertThat(device.getUpdatedAtUtc()).isEqualTo(activatedAt);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void activate_whenPublicKeyIsEmptyOrNull_shouldThrowIllegalArgumentException(String invalidKey) {
        Device device = new Device(VALID_SERIAL, NOW_UTC, null, null, null);

        assertThatThrownBy(() -> device.activate(invalidKey, VALID_ALGORITHM, NOW_UTC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(Device.ErrorMessages.EMPTY_PUBLIC_KEY);
    }

    @Test
    void activate_whenDeviceIsRevoked_shouldThrowIllegalStateException() {
        Device device = new Device(VALID_SERIAL, NOW_UTC, null, null, null);
        device.revoke(NOW_UTC.plusSeconds(60), "Compromised");

        assertThatThrownBy(() -> device.activate(VALID_PUBLIC_KEY, VALID_ALGORITHM, NOW_UTC.plusSeconds(120)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(Device.ErrorMessages.DEVICE_ALREADY_REVOKED);
    }

    @Test
    void revoke_whenDeviceIsActive_shouldTransitionToRevokedWithNotes() {
        Device device = Device.enroll(VALID_SERIAL, VALID_PUBLIC_KEY, NOW_UTC, VALID_ALGORITHM, UUID.randomUUID());
        Instant revokedAt = NOW_UTC.plusSeconds(7200);

        device.revoke(revokedAt, "Tamper sensor triggered");

        assertThat(device.getStatus()).isEqualTo(DeviceStatus.REVOKED);
        assertThat(device.getRevokedAtUtc()).isEqualTo(revokedAt);
        assertThat(device.getUpdatedAtUtc()).isEqualTo(revokedAt);
        assertThat(device.getNotes()).contains("Tamper sensor triggered");
    }

    @Test
    void revoke_whenDeviceIsAlreadyRevoked_shouldThrowIllegalStateException() {
        Device device = new Device(VALID_SERIAL, NOW_UTC, null, null, null);
        device.revoke(NOW_UTC.plusSeconds(60), "First revocation");

        assertThatThrownBy(() -> device.revoke(NOW_UTC.plusSeconds(120), "Second revocation"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(Device.ErrorMessages.DEVICE_ALREADY_REVOKED);
    }

    @Test
    void verifyAndIncrementCounter_whenCounterIsStrictlyIncreasing_shouldUpdateLastCounter() {
        Device device = Device.enroll(VALID_SERIAL, VALID_PUBLIC_KEY, NOW_UTC, VALID_ALGORITHM, UUID.randomUUID());

        device.verifyAndIncrementCounter(1);
        device.verifyAndIncrementCounter(5);
        device.verifyAndIncrementCounter(42);

        assertThat(device.getLastCounter()).isEqualTo(42L);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 10L, 5L})
    void verifyAndIncrementCounter_whenCounterIsEqualOrLowerThanLast_shouldThrowIllegalStateException(long invalidCounter) {
        Device device = Device.enroll(VALID_SERIAL, VALID_PUBLIC_KEY, NOW_UTC, VALID_ALGORITHM, UUID.randomUUID());
        device.verifyAndIncrementCounter(10);

        assertThatThrownBy(() -> device.verifyAndIncrementCounter(invalidCounter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("strictly greater");
    }

    @Test
    void verifyAndIncrementCounter_whenDeviceIsNotActive_shouldThrowIllegalStateException() {
        Device device = new Device(VALID_SERIAL, NOW_UTC, null, null, null);

        assertThatThrownBy(() -> device.verifyAndIncrementCounter(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(Device.ErrorMessages.DEVICE_NOT_ACTIVE);
    }

    @Test
    void verifyAndIncrementCounter_whenDeviceIsRevoked_shouldThrowIllegalStateException() {
        Device device = Device.enroll(VALID_SERIAL, VALID_PUBLIC_KEY, NOW_UTC, VALID_ALGORITHM, UUID.randomUUID());
        device.revoke(NOW_UTC.plusSeconds(60), "Revocation");

        assertThatThrownBy(() -> device.verifyAndIncrementCounter(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(Device.ErrorMessages.DEVICE_ALREADY_REVOKED);
    }

    @Test
    void recordHeartbeat_whenDeviceIsActive_shouldUpdateLastSeenAndVersion() {
        Device device = Device.enroll(VALID_SERIAL, VALID_PUBLIC_KEY, NOW_UTC, VALID_ALGORITHM, UUID.randomUUID());
        Instant heartbeatTime = NOW_UTC.plusSeconds(1800);

        device.recordHeartbeat(heartbeatTime, "2.0.0-rc1");

        assertThat(device.getLastSeenAtUtc()).isEqualTo(heartbeatTime);
        assertThat(device.getClientVersion()).isEqualTo("2.0.0-rc1");
        assertThat(device.getUpdatedAtUtc()).isEqualTo(heartbeatTime);
    }

    @Test
    void recordHeartbeat_whenDeviceIsRevoked_shouldThrowIllegalStateException() {
        Device device = new Device(VALID_SERIAL, NOW_UTC, null, null, null);
        device.revoke(NOW_UTC.plusSeconds(60), "Revoked");

        assertThatThrownBy(() -> device.recordHeartbeat(NOW_UTC.plusSeconds(120), "1.0.0"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(Device.ErrorMessages.DEVICE_ALREADY_REVOKED);
    }

    @Test
    void enroll_withValidArguments_shouldReturnEnrolledActiveDevice() {
        UUID customId = UUID.randomUUID();
        Device device = Device.enroll(VALID_SERIAL, VALID_PUBLIC_KEY, NOW_UTC, VALID_ALGORITHM, customId);

        assertThat(device.getDeviceId()).isEqualTo(customId);
        assertThat(device.getSerialPhysical()).isEqualTo(VALID_SERIAL);
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.ACTIVE);
        assertThat(device.getPublicKey()).isEqualTo(VALID_PUBLIC_KEY);
        assertThat(device.getKeyAlgorithm()).isEqualTo(VALID_ALGORITHM);
        assertThat(device.getActivatedAtUtc()).isEqualTo(NOW_UTC);
    }
}

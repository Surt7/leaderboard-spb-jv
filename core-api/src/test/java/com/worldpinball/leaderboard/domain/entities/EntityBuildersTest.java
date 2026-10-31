package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.DeviceStatus;
import com.worldpinball.leaderboard.domain.enums.GameSessionState;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import com.worldpinball.leaderboard.domain.enums.TelemetryMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Entity Builders Tests")
class EntityBuildersTest {

    @Test
    @DisplayName("Machine.builder() constructs valid machine with optional fields")
    void machineBuilder_createsValidMachine() {
        UUID machineId = UUID.randomUUID();
        UUID ipId = UUID.randomUUID();
        Instant now = Instant.now();

        Machine machine = Machine.builder()
                .machineId(machineId)
                .ipId(ipId)
                .worldPinballSerialNumber("WP-125-RE-DO")
                .displayName("Godzilla Pro")
                .manufacturer("Stern")
                .location("Main Arcade")
                .status(MachineStatus.ACTIVE)
                .registeredAtUtc(now)
                .build();

        assertThat(machine.getMachineId()).isEqualTo(machineId);
        assertThat(machine.getIpId()).isEqualTo(ipId);
        assertThat(machine.getDisplayName()).isEqualTo("Godzilla Pro");
        assertThat(machine.getWorldPinballSerialNumber()).isEqualTo("WP-125-RE-DO");
        assertThat(machine.getManufacturer()).isEqualTo("Stern");
        assertThat(machine.getLocation()).isEqualTo("Main Arcade");
        assertThat(machine.getStatus()).isEqualTo(MachineStatus.ACTIVE);
    }

    @Test
    @DisplayName("Device.builder() constructs valid device with all security parameters")
    void deviceBuilder_createsValidDevice() {
        UUID deviceId = UUID.randomUUID();
        Instant now = Instant.now();

        Device device = Device.builder()
                .deviceId(deviceId)
                .serialPhysical("DEV-SN-999")
                .status(DeviceStatus.ACTIVE)
                .publicKey("mock-pub-key")
                .keyAlgorithm("ecdsa-p256-sha256")
                .clientVersion("1.0.0")
                .createdAtUtc(now)
                .build();

        assertThat(device.getDeviceId()).isEqualTo(deviceId);
        assertThat(device.getSerialPhysical()).isEqualTo("DEV-SN-999");
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.ACTIVE);
        assertThat(device.getPublicKey()).isEqualTo("mock-pub-key");
        assertThat(device.getKeyAlgorithm()).isEqualTo("ecdsa-p256-sha256");
    }

    @Test
    @DisplayName("GameSession.builder() constructs valid session with state and mode")
    void gameSessionBuilder_createsValidSession() {
        UUID sessionId = UUID.randomUUID();
        UUID machineId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID modeId = UUID.randomUUID();

        GameSession session = GameSession.builder()
                .sessionId(sessionId)
                .machineId(machineId)
                .deviceId(deviceId)
                .modeId(modeId)
                .clientVersion("1.0.0")
                .nonce("session-nonce-123")
                .state(GameSessionState.STARTED)
                .telemetryMode(TelemetryMode.LIVE_STREAMING)
                .build();

        assertThat(session.getSessionId()).isEqualTo(sessionId);
        assertThat(session.getMachineId()).isEqualTo(machineId);
        assertThat(session.getDeviceId()).isEqualTo(deviceId);
        assertThat(session.getState()).isEqualTo(GameSessionState.STARTED);
        assertThat(session.getTelemetryMode()).isEqualTo(TelemetryMode.LIVE_STREAMING);
    }

    @Test
    @DisplayName("ScoreEntry.builder() constructs score entry with validated flag")
    void scoreEntryBuilder_createsValidScoreEntry() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID machineId = UUID.randomUUID();
        UUID ipId = UUID.randomUUID();
        UUID modeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ScoreEntry entry = ScoreEntry.builder()
                .sessionId(sessionId)
                .slotNumber(1)
                .deviceId(deviceId)
                .machineId(machineId)
                .ipId(ipId)
                .modeId(modeId)
                .userId(userId)
                .score(1_500_000L)
                .validated(true)
                .build();

        assertThat(entry.getSessionId()).isEqualTo(sessionId);
        assertThat(entry.getScore()).isEqualTo(1_500_000L);
        assertThat(entry.isValidated()).isTrue();
        assertThat(entry.getSlotNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("UserProfile.builder() constructs valid user profile")
    void userProfileBuilder_createsValidProfile() {
        UUID userId = UUID.randomUUID();

        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .displayName("PinballWizard")
                .initials("PBW")
                .countryCode("FR")
                .profilePublic(true)
                .build();

        assertThat(profile.getUserId()).isEqualTo(userId);
        assertThat(profile.getDisplayName()).isEqualTo("PinballWizard");
        assertThat(profile.getInitials()).isEqualTo("PBW");
        assertThat(profile.isProfilePublic()).isTrue();
    }

    @Test
    @DisplayName("IpLicense.builder() and GameMode.builder() construct domain models cleanly")
    void licenseAndModeBuilders_createEntities() {
        IpLicense license = IpLicense.builder()
                .name("Godzilla")
                .slug("godzilla")
                .active(true)
                .build();

        assertThat(license.getName()).isEqualTo("Godzilla");
        assertThat(license.getSlug()).isEqualTo("godzilla");
        assertThat(license.isActive()).isTrue();

        GameMode mode = GameMode.builder()
                .code("classic")
                .name("Classic Mode")
                .description("3 balls classic")
                .active(true)
                .build();

        assertThat(mode.getCode()).isEqualTo("classic");
        assertThat(mode.getName()).isEqualTo("Classic Mode");
        assertThat(mode.isActive()).isTrue();
    }
}

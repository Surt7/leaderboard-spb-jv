package com.worldpinball.leaderboard.application.features.gamesessions.Public;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession.CreateGameSessionHandlerImpl;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession.CreateGameSessionResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession.commands.CreateGameSessionCommand;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.domain.entities.Device;
import com.worldpinball.leaderboard.domain.entities.GameMode;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.domain.entities.MachineDevice;
import com.worldpinball.leaderboard.domain.enums.DeviceStatus;
import com.worldpinball.leaderboard.infrastructure.DeviceRepository;
import com.worldpinball.leaderboard.infrastructure.GameModeRepository;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import com.worldpinball.leaderboard.infrastructure.MachineDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateGameSessionHandler Tests")
class CreateGameSessionHandlerTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private MachineDeviceRepository machineDeviceRepository;

    @Mock
    private GameModeRepository gameModeRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private LobbyNotificationService lobbyNotifier;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneOffset.UTC);

    private CreateGameSessionHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new CreateGameSessionHandlerImpl(
                deviceRepository,
                machineDeviceRepository,
                gameModeRepository,
                gameSessionRepository,
                lobbyNotifier,
                clock
        );
    }

    @Test
    @DisplayName("Returns Success when device, machine attachment and game mode are valid")
    void handle_whenValidRequest_shouldReturnSuccessResult() {
        UUID deviceId = UUID.randomUUID();
        UUID machineId = UUID.randomUUID();
        UUID modeId = UUID.randomUUID();
        String modeCode = "CLASSIC";
        String clientVersion = "1.0.0";

        Device device = Device.enroll("DEV-SN-001", "PUB_KEY", Instant.now(clock));
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        MachineDevice machineDevice = new MachineDevice(machineId, deviceId, Instant.now(clock));
        when(machineDeviceRepository.findByDeviceIdAndActiveTrue(deviceId)).thenReturn(Optional.of(machineDevice));

        GameMode gameMode = new GameMode(modeCode, "Classic Mode", "Classic description", true, Instant.now(clock), modeId);
        when(gameModeRepository.findByCodeIgnoreCaseAndActiveTrue(modeCode)).thenReturn(Optional.of(gameMode));

        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateGameSessionCommand command = new CreateGameSessionCommand(deviceId, modeCode, clientVersion);
        CreateGameSessionResult result = sut.handle(command);

        assertThat(result).isInstanceOf(CreateGameSessionResult.Success.class);
        CreateGameSessionResult.Success success = (CreateGameSessionResult.Success) result;
        assertThat(success.sessionId()).isNotNull();
        assertThat(success.nonce()).isNotBlank();
        assertThat(success.createdAtUtc()).isEqualTo(Instant.now(clock));

        verify(gameSessionRepository).save(any(GameSession.class));
        verify(lobbyNotifier).notifyLobbyUpdated(success.sessionId());
    }

    @Test
    @DisplayName("Returns DeviceNotFound when device does not exist")
    void handle_whenDeviceNotFound_shouldReturnDeviceNotFoundResult() {
        UUID deviceId = UUID.randomUUID();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        CreateGameSessionCommand command = new CreateGameSessionCommand(deviceId, "CLASSIC", "1.0.0");
        CreateGameSessionResult result = sut.handle(command);

        assertThat(result).isInstanceOf(CreateGameSessionResult.DeviceNotFound.class);
        CreateGameSessionResult.DeviceNotFound notFound = (CreateGameSessionResult.DeviceNotFound) result;
        assertThat(notFound.deviceId()).isEqualTo(deviceId);

        verifyNoInteractions(machineDeviceRepository);
        verifyNoInteractions(gameModeRepository);
        verifyNoInteractions(gameSessionRepository);
    }

    @ParameterizedTest
    @EnumSource(value = DeviceStatus.class, names = {"PENDING", "REVOKED"})
    @DisplayName("Returns DeviceNotActive when device status is not ACTIVE")
    void handle_whenDeviceNotActive_shouldReturnDeviceNotActiveResult(DeviceStatus status) {
        UUID deviceId = UUID.randomUUID();
        Device device;
        if (status == DeviceStatus.REVOKED) {
            device = Device.enroll("DEV-SN-001", "PUB_KEY", Instant.now(clock));
            device.revoke(Instant.now(clock), "revoked for test");
        } else {
            device = new Device("DEV-SN-001", Instant.now(clock), deviceId, "1.0", "notes");
        }

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        CreateGameSessionCommand command = new CreateGameSessionCommand(deviceId, "CLASSIC", "1.0.0");
        CreateGameSessionResult result = sut.handle(command);

        assertThat(result).isInstanceOf(CreateGameSessionResult.DeviceNotActive.class);
        CreateGameSessionResult.DeviceNotActive notActive = (CreateGameSessionResult.DeviceNotActive) result;
        assertThat(notActive.deviceId()).isEqualTo(deviceId);
    }

    @Test
    @DisplayName("Returns MachineNotAttached when device is active but has no attached machine")
    void handle_whenMachineNotAttached_shouldReturnMachineNotAttachedResult() {
        UUID deviceId = UUID.randomUUID();
        Device device = Device.enroll("DEV-SN-001", "PUB_KEY", Instant.now(clock));
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(machineDeviceRepository.findByDeviceIdAndActiveTrue(deviceId)).thenReturn(Optional.empty());

        CreateGameSessionCommand command = new CreateGameSessionCommand(deviceId, "CLASSIC", "1.0.0");
        CreateGameSessionResult result = sut.handle(command);

        assertThat(result).isInstanceOf(CreateGameSessionResult.MachineNotAttached.class);
        CreateGameSessionResult.MachineNotAttached notAttached = (CreateGameSessionResult.MachineNotAttached) result;
        assertThat(notAttached.deviceId()).isEqualTo(deviceId);
    }

    @Test
    @DisplayName("Returns ModeNotFound when game mode code does not match an active mode")
    void handle_whenModeNotFound_shouldReturnModeNotFoundResult() {
        UUID deviceId = UUID.randomUUID();
        UUID machineId = UUID.randomUUID();
        String unknownMode = "UNKNOWN_MODE";

        Device device = Device.enroll("DEV-SN-001", "PUB_KEY", Instant.now(clock));
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        MachineDevice machineDevice = new MachineDevice(machineId, deviceId, Instant.now(clock));
        when(machineDeviceRepository.findByDeviceIdAndActiveTrue(deviceId)).thenReturn(Optional.of(machineDevice));

        when(gameModeRepository.findByCodeIgnoreCaseAndActiveTrue(unknownMode)).thenReturn(Optional.empty());

        CreateGameSessionCommand command = new CreateGameSessionCommand(deviceId, unknownMode, "1.0.0");
        CreateGameSessionResult result = sut.handle(command);

        assertThat(result).isInstanceOf(CreateGameSessionResult.ModeNotFound.class);
        CreateGameSessionResult.ModeNotFound notFound = (CreateGameSessionResult.ModeNotFound) result;
        assertThat(notFound.modeCode()).isEqualTo(unknownMode);
    }
}

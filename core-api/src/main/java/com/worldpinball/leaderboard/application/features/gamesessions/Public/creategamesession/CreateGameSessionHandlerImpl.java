package com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession.commands.CreateGameSessionCommand;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.common.EventId;
import com.worldpinball.leaderboard.domain.entities.Device;
import com.worldpinball.leaderboard.domain.entities.GameMode;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.domain.entities.MachineDevice;
import com.worldpinball.leaderboard.domain.enums.DeviceStatus;
import com.worldpinball.leaderboard.infrastructure.DeviceRepository;
import com.worldpinball.leaderboard.infrastructure.GameModeRepository;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import com.worldpinball.leaderboard.infrastructure.MachineDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateGameSessionHandlerImpl implements CreateGameSessionHandler {

    private static final EventId DEVICE_NOT_FOUND = new EventId(2300, "DeviceNotFound");
    private static final EventId DEVICE_NOT_ACTIVE = new EventId(2301, "DeviceNotActive");
    private static final EventId MACHINE_NOT_ATTACHED = new EventId(2302, "MachineNotAttached");
    private static final EventId MODE_NOT_FOUND = new EventId(2303, "ModeNotFound");
    private static final EventId SESSION_CREATED = new EventId(2304, "SessionCreated");

    private final DeviceRepository deviceRepository;
    private final MachineDeviceRepository machineDeviceRepository;
    private final GameModeRepository gameModeRepository;
    private final GameSessionRepository gameSessionRepository;
    private final LobbyNotificationService lobbyNotifier;
    private final Clock clock;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public CreateGameSessionResult handle(CreateGameSessionCommand command) {
        Instant nowUtc = Instant.now(clock);

        Optional<Device> deviceOpt = deviceRepository.findById(command.deviceId());
        if (deviceOpt.isEmpty()) {
            log.warn("{} Device {} could not be found.", DEVICE_NOT_FOUND.tag(), command.deviceId());
            return new CreateGameSessionResult.DeviceNotFound(command.deviceId());
        }

        Device device = deviceOpt.get();
        if (device.getStatus() != DeviceStatus.ACTIVE) {
            log.warn("{} Device {} is not active.", DEVICE_NOT_ACTIVE.tag(), command.deviceId());
            return new CreateGameSessionResult.DeviceNotActive(command.deviceId());
        }

        Optional<MachineDevice> machineDeviceOpt = machineDeviceRepository.findByDeviceIdAndActiveTrue(command.deviceId());
        if (machineDeviceOpt.isEmpty()) {
            log.warn("{} Device {} has no active machine attached.", MACHINE_NOT_ATTACHED.tag(), command.deviceId());
            return new CreateGameSessionResult.MachineNotAttached(command.deviceId());
        }

        Optional<GameMode> modeOpt = gameModeRepository.findByCodeIgnoreCaseAndActiveTrue(command.modeCode());
        if (modeOpt.isEmpty()) {
            log.warn("{} Game mode {} could not be found or is inactive.", MODE_NOT_FOUND.tag(), command.modeCode());
            return new CreateGameSessionResult.ModeNotFound(command.modeCode());
        }

        GameMode mode = modeOpt.get();
        MachineDevice machineDevice = machineDeviceOpt.get();

        byte[] nonceBytes = new byte[16];
        secureRandom.nextBytes(nonceBytes);
        String nonce = HexFormat.of().formatHex(nonceBytes);

        GameSession session = new GameSession(
            machineDevice.getMachineId(),
            command.deviceId(),
            mode.getModeId(),
            command.clientVersion(),
            nonce,
            nowUtc,
            null
        );

        GameSession saved = gameSessionRepository.save(session);
        lobbyNotifier.notifyLobbyUpdated(saved.getSessionId());

        log.info("{} Created game session {} for machine {} and device {}.",
            SESSION_CREATED.tag(), saved.getSessionId(), machineDevice.getMachineId(), command.deviceId());

        return new CreateGameSessionResult.Success(saved.getSessionId(), nonce, nowUtc);
    }
}

package com.worldpinball.leaderboard.api.graphql;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.AddPlayerHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.AddPlayerResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.commands.AddPlayerCommand;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.domain.entities.Device;
import com.worldpinball.leaderboard.domain.entities.GameMode;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.domain.entities.GameSessionPlayer;
import com.worldpinball.leaderboard.domain.entities.IpLicense;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.entities.MachineDevice;
import com.worldpinball.leaderboard.domain.entities.ScoreEntry;
import com.worldpinball.leaderboard.domain.entities.UserProfile;
import com.worldpinball.leaderboard.domain.enums.DeviceStatus;
import com.worldpinball.leaderboard.infrastructure.DeviceRepository;
import com.worldpinball.leaderboard.infrastructure.GameModeRepository;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import com.worldpinball.leaderboard.infrastructure.IpLicenseRepository;
import com.worldpinball.leaderboard.infrastructure.MachineDeviceRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import com.worldpinball.leaderboard.infrastructure.ScoreEntryRepository;
import com.worldpinball.leaderboard.infrastructure.UserProfileRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class GraphQlController {

    public record ScoreStatsDto(int totalGames, long bestScore) {}

    public record ActiveSessionDto(
            UUID sessionId,
            String machineName,
            String modeName,
            int slotNumber,
            String state,
            String startedAtUtc
    ) {}

    public record ClaimMachinePayload(
            boolean success,
            String message,
            Machine machine
    ) {}

    public record JoinGameSessionPayload(
            boolean success,
            String message,
            Integer slotNumber,
            String machineName
    ) {}

    private final ScoreEntryRepository scoreEntryRepository;
    private final GameModeRepository gameModeRepository;
    private final IpLicenseRepository ipLicenseRepository;
    private final MachineRepository machineRepository;
    private final DeviceRepository deviceRepository;
    private final MachineDeviceRepository machineDeviceRepository;
    private final UserProfileRepository userProfileRepository;
    private final GameSessionRepository gameSessionRepository;
    private final AddPlayerHandler addPlayerHandler;
    private final LobbyNotificationService lobbyNotifier;

    public GraphQlController(
            ScoreEntryRepository scoreEntryRepository,
            GameModeRepository gameModeRepository,
            IpLicenseRepository ipLicenseRepository,
            MachineRepository machineRepository,
            DeviceRepository deviceRepository,
            MachineDeviceRepository machineDeviceRepository,
            UserProfileRepository userProfileRepository,
            GameSessionRepository gameSessionRepository,
            AddPlayerHandler addPlayerHandler,
            LobbyNotificationService lobbyNotifier
    ) {
        this.scoreEntryRepository = scoreEntryRepository;
        this.gameModeRepository = gameModeRepository;
        this.ipLicenseRepository = ipLicenseRepository;
        this.machineRepository = machineRepository;
        this.deviceRepository = deviceRepository;
        this.machineDeviceRepository = machineDeviceRepository;
        this.userProfileRepository = userProfileRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.addPlayerHandler = addPlayerHandler;
        this.lobbyNotifier = lobbyNotifier;
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public ScoreStatsDto myScoreStats(@Argument String modeCode, Principal principal) {
        UUID userId = resolveUserUuid(principal);
        if (userId == null) {
            return new ScoreStatsDto(0, 0L);
        }

        if (modeCode != null && !modeCode.isBlank() && !modeCode.equalsIgnoreCase("all")) {
            Optional<GameMode> modeOpt = gameModeRepository.findByCode(modeCode.trim().toLowerCase());
            if (modeOpt.isEmpty()) {
                return new ScoreStatsDto(0, 0L);
            }
            UUID modeId = modeOpt.get().getModeId();
            long total = scoreEntryRepository.countByUserIdAndModeId(userId, modeId);
            long best = total > 0 ? scoreEntryRepository.findMaxScoreByUserIdAndModeId(userId, modeId) : 0L;
            return new ScoreStatsDto((int) total, best);
        }

        long total = scoreEntryRepository.countByUserId(userId);
        long best = total > 0 ? scoreEntryRepository.findMaxScoreByUserId(userId) : 0L;
        return new ScoreStatsDto((int) total, best);
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<ScoreEntry> myScores(
            @Argument Integer skip,
            @Argument Integer take,
            @Argument Map<String, Object> where,
            @Argument List<Map<String, String>> order,
            Principal principal
    ) {
        UUID userId = resolveUserUuid(principal);
        if (userId == null) {
            return List.of();
        }

        int safeSkip = skip != null && skip > 0 ? skip : 0;
        int safeTake = take != null ? Math.min(Math.max(take, 1), 50) : 20;

        Stream<ScoreEntry> stream = scoreEntryRepository.findByUserId(userId).stream();
        stream = applyScoreFilter(stream, where);
        stream = applyScoreSort(stream, order);

        return stream.skip(safeSkip).limit(safeTake).toList();
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<ScoreEntry> scores(
            @Argument Integer skip,
            @Argument Integer take,
            @Argument Map<String, Object> where,
            @Argument List<Map<String, String>> order
    ) {
        int safeSkip = skip != null && skip > 0 ? skip : 0;
        int safeTake = take != null ? Math.min(Math.max(take, 1), 100) : 50;

        Stream<ScoreEntry> stream = scoreEntryRepository.findAll().stream();
        stream = applyScoreFilter(stream, where);
        stream = applyScoreSort(stream, order);

        return stream.skip(safeSkip).limit(safeTake).toList();
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Machine> machines(
            @Argument Map<String, Object> where,
            @Argument List<Map<String, String>> order,
            Principal principal
    ) {
        UUID userId = resolveUserUuid(principal);
        if (userId == null) {
            return List.of();
        }

        List<Device> userDevices = deviceRepository.findByOwnerUserId(userId);
        if (userDevices.isEmpty()) {
            return List.of();
        }

        List<UUID> deviceIds = userDevices.stream().map(Device::getDeviceId).toList();
        List<MachineDevice> activeLinks = machineDeviceRepository.findByDeviceIdInAndActiveTrue(deviceIds);
        if (activeLinks.isEmpty()) {
            return List.of();
        }

        List<UUID> machineIds = activeLinks.stream().map(MachineDevice::getMachineId).distinct().toList();
        Stream<Machine> stream = machineRepository.findAllById(machineIds).stream();
        stream = applyMachineSort(stream, order);

        return stream.limit(50).toList();
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<UserProfile> profiles(Principal principal) {
        UUID userId = resolveUserUuid(principal);
        if (userId != null) {
            return userProfileRepository.findById(userId).map(List::of).orElse(List.of());
        }
        return userProfileRepository.findByProfilePublicTrue().stream().limit(50).toList();
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public ActiveSessionDto myActiveSession(Principal principal) {
        UUID userId = resolveUserUuid(principal);
        if (userId == null) {
            return null;
        }

        List<GameSession> sessions = gameSessionRepository.findActiveSessionsForUser(userId);
        if (sessions.isEmpty()) {
            return null;
        }

        GameSession session = sessions.getFirst();
        Optional<GameSessionPlayer> playerOpt = session.getPlayers().stream()
                .filter(p -> Objects.equals(p.getUserId(), userId))
                .findFirst();

        if (playerOpt.isEmpty()) {
            return null;
        }

        String machineName = machineRepository.findById(session.getMachineId())
                .map(Machine::getDisplayName)
                .orElse("Pinball Machine");
        String modeName = gameModeRepository.findById(session.getModeId())
                .map(GameMode::getName)
                .orElse(null);

        return new ActiveSessionDto(
                session.getSessionId(),
                machineName,
                modeName,
                playerOpt.get().getSlotNumber(),
                session.getState().name(),
                session.getStartedAtUtc().toString()
        );
    }

    @MutationMapping
    @Transactional
    public ClaimMachinePayload claimMachine(@Argument String code, Principal principal) {
        UUID userId = resolveUserUuid(principal);
        if (userId == null) {
            return new ClaimMachinePayload(false, "Utilisateur non authentifié.", null);
        }

        if (code == null || code.isBlank()) {
            return new ClaimMachinePayload(false, "Veuillez fournir un code d'activation.", null);
        }

        String trimmedCode = code.trim();
        String targetSerial = normalizeDeviceSerial(trimmedCode);

        Optional<Device> deviceOpt = deviceRepository.findBySerialPhysical(trimmedCode);
        if (deviceOpt.isEmpty() && targetSerial != null) {
            deviceOpt = deviceRepository.findBySerialPhysical(targetSerial);
        }

        if (deviceOpt.isEmpty()) {
            return new ClaimMachinePayload(false, "Aucune machine trouvée pour ce code d'activation.", null);
        }

        Device device = deviceOpt.get();
        if (device.getStatus() == DeviceStatus.REVOKED) {
            return new ClaimMachinePayload(false, "Ce flipper est désactivé ou révoqué.", null);
        }

        Machine activeMachine = machineDeviceRepository.findFirstByDeviceIdAndActiveTrue(device.getDeviceId())
                .flatMap(md -> machineRepository.findById(md.getMachineId()))
                .orElse(null);

        if (device.getOwnerUserId() != null) {
            if (device.getOwnerUserId().equals(userId)) {
                return new ClaimMachinePayload(false, "Vous possédez déjà ce flipper.", activeMachine);
            }
            return new ClaimMachinePayload(false, "Cette machine est déjà associée à un autre propriétaire.", null);
        }

        device.assignOwner(userId);
        deviceRepository.save(device);

        String machineName = activeMachine != null ? activeMachine.getDisplayName() : device.getSerialPhysical();
        return new ClaimMachinePayload(
                true,
                "Le flipper \"" + machineName + "\" a été relié avec succès à votre compte !",
                activeMachine
        );
    }

    @MutationMapping
    @Transactional
    public JoinGameSessionPayload joinGameSession(@Argument String code, Principal principal) {
        UUID userId = resolveUserUuid(principal);
        if (userId == null) {
            return new JoinGameSessionPayload(false, "Authentication required to join a game.", null, null);
        }

        if (code == null || code.isBlank()) {
            return new JoinGameSessionPayload(false, "Session or machine code cannot be empty.", null, null);
        }

        String trimmedCode = code.trim();
        String targetSerial = normalizeDeviceSerial(trimmedCode);
        if (targetSerial == null) {
            targetSerial = trimmedCode.toUpperCase();
        }

        GameSession session = null;

        // Path A: Direct session GUID
        try {
            UUID directSessionId = UUID.fromString(trimmedCode);
            session = gameSessionRepository.findByIdWithPlayers(directSessionId).orElse(null);
        } catch (IllegalArgumentException ignored) {
        }

        // Path B: Machine/Device code
        if (session == null) {
            Optional<Device> deviceOpt = deviceRepository.findBySerialPhysical(trimmedCode);
            if (deviceOpt.isEmpty()) {
                deviceOpt = deviceRepository.findBySerialPhysical(targetSerial);
            }
            if (deviceOpt.isPresent()) {
                Optional<MachineDevice> linkOpt = machineDeviceRepository.findFirstByDeviceIdAndActiveTrue(deviceOpt.get().getDeviceId());
                if (linkOpt.isPresent()) {
                    List<GameSession> activeSessions = gameSessionRepository.findActiveSessionsByMachineIds(
                            List.of(linkOpt.get().getMachineId())
                    );
                    if (!activeSessions.isEmpty()) {
                        session = activeSessions.getFirst();
                    }
                }
            }
        }

        if (session == null) {
            return new JoinGameSessionPayload(false, "No active game session found for this pinball machine.", null, null);
        }

        String machineName = machineRepository.findById(session.getMachineId())
                .map(Machine::getDisplayName)
                .orElse("Pinball Machine");

        Optional<GameSessionPlayer> existingPlayer = session.getPlayers().stream()
                .filter(p -> Objects.equals(p.getUserId(), userId))
                .findFirst();
        if (existingPlayer.isPresent()) {
            int slotNum = existingPlayer.get().getSlotNumber();
            return new JoinGameSessionPayload(
                    true,
                    "You are already in this game as Player " + slotNum + ".",
                    slotNum,
                    machineName
            );
        }

        Set<Integer> occupiedSlots = session.getPlayers().stream()
                .map(GameSessionPlayer::getSlotNumber)
                .collect(Collectors.toSet());
        Integer freeSlot = null;
        for (int i = 1; i <= 4; i++) {
            if (!occupiedSlots.contains(i)) {
                freeSlot = i;
                break;
            }
        }

        if (freeSlot == null) {
            return new JoinGameSessionPayload(false, "This game session is already full (4 players max).", null, null);
        }

        AddPlayerCommand command = new AddPlayerCommand(session.getSessionId(), freeSlot, userId, null, false);
        AddPlayerResult result = addPlayerHandler.handle(command);
        if (result instanceof AddPlayerResult.Success success) {
            lobbyNotifier.notifyLobbyUpdated(session.getSessionId());
            return new JoinGameSessionPayload(
                    true,
                    "Joined " + machineName + " as Player " + success.slotNumber() + "!",
                    success.slotNumber(),
                    machineName
            );
        }

        return new JoinGameSessionPayload(false, "Unable to join session at this moment.", null, null);
    }

    @MutationMapping
    @Transactional
    public boolean endGameSession(@Argument UUID sessionId, Principal principal) {
        UUID userId = resolveUserUuid(principal);
        if (userId == null || sessionId == null) {
            return false;
        }

        Optional<GameSession> sessionOpt = gameSessionRepository.findByIdWithPlayers(sessionId);
        if (sessionOpt.isEmpty()) {
            return false;
        }

        GameSession session = sessionOpt.get();
        boolean isParticipant = session.getPlayers().stream()
                .anyMatch(p -> Objects.equals(p.getUserId(), userId));
        if (!isParticipant) {
            return false;
        }

        session.abort(Instant.now());
        gameSessionRepository.save(session);
        lobbyNotifier.notifyLobbyUpdated(sessionId);
        return true;
    }

    // --- Schema Mappings for nested relations and C# naming compatibility ---

    @SchemaMapping(typeName = "ScoreEntry", field = "isValidated")
    public boolean scoreIsValidated(ScoreEntry scoreEntry) {
        return scoreEntry.isValidated();
    }

    @SchemaMapping(typeName = "ScoreEntry", field = "userId")
    public String scoreUserId(ScoreEntry scoreEntry) {
        return scoreEntry.getUserId() != null ? scoreEntry.getUserId().toString() : null;
    }

    @SchemaMapping(typeName = "ScoreEntry", field = "machine")
    public Machine scoreMachine(ScoreEntry scoreEntry) {
        return machineRepository.findById(scoreEntry.getMachineId()).orElse(null);
    }

    @SchemaMapping(typeName = "ScoreEntry", field = "device")
    public Device scoreDevice(ScoreEntry scoreEntry) {
        return deviceRepository.findById(scoreEntry.getDeviceId()).orElse(null);
    }

    @SchemaMapping(typeName = "ScoreEntry", field = "ipLicense")
    public IpLicense scoreIpLicense(ScoreEntry scoreEntry) {
        return ipLicenseRepository.findById(scoreEntry.getIpId()).orElse(null);
    }

    @SchemaMapping(typeName = "ScoreEntry", field = "mode")
    public GameMode scoreMode(ScoreEntry scoreEntry) {
        return gameModeRepository.findById(scoreEntry.getModeId()).orElse(null);
    }

    @SchemaMapping(typeName = "Machine", field = "ipLicense")
    public IpLicense machineIpLicense(Machine machine) {
        return ipLicenseRepository.findById(machine.getIpId()).orElse(null);
    }

    @SchemaMapping(typeName = "IpLicense", field = "isActive")
    public boolean ipLicenseIsActive(IpLicense ipLicense) {
        return ipLicense.isActive();
    }

    @SchemaMapping(typeName = "GameMode", field = "isActive")
    public boolean gameModeIsActive(GameMode gameMode) {
        return gameMode.isActive();
    }

    @SchemaMapping(typeName = "UserProfile", field = "isProfilePublic")
    public boolean userProfileIsPublic(UserProfile userProfile) {
        return userProfile.isProfilePublic();
    }

    // --- Helpers ---

    private static UUID resolveUserUuid(Principal principal) {
        String sub = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            sub = jwt.getSubject();
        }
        if ((sub == null || sub.isBlank()) && principal != null) {
            sub = principal.getName();
        }
        if (sub == null || sub.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(sub.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String normalizeDeviceSerial(String trimmedCode) {
        if (trimmedCode.regionMatches(true, 0, "WP-", 0, 3)) {
            String numStr = trimmedCode.substring(3).trim();
            try {
                int num = Integer.parseInt(numStr);
                return String.format("DEVICE-%05d", num);
            } catch (NumberFormatException ignored) {
            }
        } else if (trimmedCode.regionMatches(true, 0, "DEVICE-", 0, 7)) {
            return trimmedCode.toUpperCase();
        } else {
            try {
                int num = Integer.parseInt(trimmedCode);
                return String.format("DEVICE-%05d", num);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Stream<ScoreEntry> applyScoreFilter(Stream<ScoreEntry> stream, Map<String, Object> where) {
        if (where == null || where.isEmpty()) {
            return stream;
        }
        Map<String, Object> modeFilter = (Map<String, Object>) where.get("mode");
        if (modeFilter != null) {
            Map<String, Object> codeFilter = (Map<String, Object>) modeFilter.get("code");
            if (codeFilter != null && codeFilter.get("eq") instanceof String codeEq) {
                Optional<GameMode> modeOpt = gameModeRepository.findByCode(codeEq.toLowerCase());
                if (modeOpt.isEmpty()) {
                    return Stream.empty();
                }
                UUID targetModeId = modeOpt.get().getModeId();
                stream = stream.filter(s -> targetModeId.equals(s.getModeId()));
            }
        }
        Map<String, Object> ipFilter = (Map<String, Object>) where.get("ipLicense");
        if (ipFilter != null) {
            Map<String, Object> slugFilter = (Map<String, Object>) ipFilter.get("slug");
            if (slugFilter != null && slugFilter.get("eq") instanceof String slugEq) {
                Optional<IpLicense> ipOpt = ipLicenseRepository.findFirstBySlugIgnoreCaseAndActiveTrue(slugEq);
                if (ipOpt.isEmpty()) {
                    return Stream.empty();
                }
                UUID targetIpId = ipOpt.get().getIpId();
                stream = stream.filter(s -> targetIpId.equals(s.getIpId()));
            }
        }
        return stream;
    }

    private static Stream<ScoreEntry> applyScoreSort(Stream<ScoreEntry> stream, List<Map<String, String>> order) {
        if (order == null || order.isEmpty()) {
            return stream.sorted(Comparator.comparingLong(ScoreEntry::getScore).reversed());
        }
        Comparator<ScoreEntry> comparator = null;
        for (Map<String, String> rule : order) {
            for (Map.Entry<String, String> entry : rule.entrySet()) {
                Comparator<ScoreEntry> c = switch (entry.getKey()) {
                    case "score" -> Comparator.comparingLong(ScoreEntry::getScore);
                    case "achievedAtUtc" -> Comparator.comparing(ScoreEntry::getAchievedAtUtc);
                    case "slotNumber" -> Comparator.comparingInt(ScoreEntry::getSlotNumber);
                    default -> null;
                };
                if (c != null) {
                    if ("DESC".equalsIgnoreCase(entry.getValue())) {
                        c = c.reversed();
                    }
                    comparator = (comparator == null) ? c : comparator.thenComparing(c);
                }
            }
        }
        return comparator != null ? stream.sorted(comparator) : stream;
    }

    private static Stream<Machine> applyMachineSort(Stream<Machine> stream, List<Map<String, String>> order) {
        if (order == null || order.isEmpty()) {
            return stream;
        }
        Comparator<Machine> comparator = null;
        for (Map<String, String> rule : order) {
            for (Map.Entry<String, String> entry : rule.entrySet()) {
                Comparator<Machine> c = switch (entry.getKey()) {
                    case "displayName" -> Comparator.comparing(Machine::getDisplayName, String.CASE_INSENSITIVE_ORDER);
                    case "registeredAtUtc" -> Comparator.comparing(Machine::getRegisteredAtUtc);
                    case "worldPinballSerialNumber" -> Comparator.comparing(Machine::getWorldPinballSerialNumber);
                    default -> null;
                };
                if (c != null) {
                    if ("DESC".equalsIgnoreCase(entry.getValue())) {
                        c = c.reversed();
                    }
                    comparator = (comparator == null) ? c : comparator.thenComparing(c);
                }
            }
        }
        return comparator != null ? stream.sorted(comparator) : stream;
    }
}

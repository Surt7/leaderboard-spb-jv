package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.GameSessionState;
import com.worldpinball.leaderboard.domain.enums.GameSlotStatus;
import com.worldpinball.leaderboard.domain.enums.TelemetryMode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "game_sessions")
@Getter
public class GameSession {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String MACHINE_ID = "MachineId cannot be empty.";
        public static final String DEVICE_ID = "DeviceId cannot be empty.";
        public static final String MODE_ID = "ModeId cannot be empty.";
        public static final String NONCE = "Nonce cannot be empty.";
        public static final String CLIENT_VERSION = "ClientVersion cannot be empty.";
        public static final String STARTED_AT_UTC = "StartedAtUtc must be a valid date.";
        public static final String END_SESSION = "Cannot end a session that is not in Started state.";
        public static final String EARLIER = "EndedAtUtc cannot be earlier than StartedAtUtc.";
        public static final String PAUSE_SESSION = "Can only pause an active session.";
        public static final String RESUME_SESSION = "Can only resume a paused session.";
        public static final String CLOSED_SESSION = "Session is already closed.";
        public static final String ADD_PLAYER_NOT_STARTED = "Cannot add a player to a session that is not in a joinable state.";
        public static final String INVALID_SLOT_NUMBER = "Slot number must be between 1 and 4.";
        public static final String MAX_PLAYERS_REACHED = "A game session cannot have more than 4 players.";
        public static final String SLOT_ALREADY_OCCUPIED = "The specified slot is already occupied.";
        public static final String USER_ALREADY_REGISTERED = "This user is already registered in this session.";
        public static final String REGISTERED_USER_REQUIRED = "A registered player (UserId) is required for this game mode or tournament.";
        public static final String CANNOT_SEARCH_PLAYERS = "Can only search for players from a created, pending, or match-found session.";
        public static final String MATCH_FOUND_NOT_SEARCHING = "Cannot mark match as found unless currently searching for players.";
        public static final String NOT_ENOUGH_PLAYERS_FOR_MATCH = "Cannot mark match as found without enough players.";
        public static final String CANNOT_REMOVE_PLAYER_ACTIVE_SESSION = "Cannot remove a player once the session has started or ended.";
        public static final String PLAYER_NOT_FOUND = "Player not found in the specified slot.";
        public static final String CANNOT_START_SESSION = "Can only start a session that is in Created or MatchFound state.";
        public static final String CANNOT_CANCEL_SESSION = "Cannot cancel a session that is already active or closed.";
        public static final String CANNOT_LAUNCH_SESSION = "Cannot launch a session that is not in a valid joinable state.";
        public static final String AT_LEAST_ONE_PLAYER_REQUIRED = "Cannot launch a session without at least one player.";
        public static final String CANNOT_RECORD_BALL_INACTIVE_SESSION = "Cannot record ball score on a session that is not active.";
        public static final String INVALID_BALL_NUMBER = "Ball number must be between 1 and 5.";
        public static final String INVALID_SCORE = "Score cannot be negative.";
    }

    @Id
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "mode_id", nullable = false)
    private UUID modeId;

    @Column(name = "started_at_utc", nullable = false)
    private Instant startedAtUtc;

    @Column(name = "ended_at_utc")
    private Instant endedAtUtc;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 20, nullable = false)
    private GameSessionState state = GameSessionState.CREATED;

    @Column(name = "nonce", length = 64, nullable = false)
    private String nonce;

    @Column(name = "client_version", length = 50)
    private String clientVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "telemetry_mode", length = 30)
    private TelemetryMode telemetryMode = TelemetryMode.BALL_CHECKPOINT_ONLY;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GameSessionPlayer> players = new ArrayList<>();

    @Transient
    private final List<GameSlot> slots = new ArrayList<>(4);

    protected GameSession() {
        for (int i = 1; i <= 4; i++) {
            this.slots.add(new GameSlot(i));
        }
    }

    @Getter
    public static final class GameSlot {
        private final int slotNumber;
        private GameSlotStatus status = GameSlotStatus.Open;
        private UUID userId;
        private String guestInitials;

        GameSlot(int slotNumber) {
            this.slotNumber = slotNumber;
        }

        void occupy(UUID userId, String initials) {
            if (this.status == GameSlotStatus.Locked) {
                throw new IllegalStateException("Locked slot.");
            }
            this.status = GameSlotStatus.Occupied;
            this.userId = userId;
            this.guestInitials = initials;
        }

        void lock() {
            if (this.status == GameSlotStatus.Open) {
                this.status = GameSlotStatus.Locked;
            }
        }

        void free() {
            if (this.status != GameSlotStatus.Locked) {
                this.status = GameSlotStatus.Open;
                this.userId = null;
                this.guestInitials = null;
            }
        }
    }

    public GameSession(
        UUID machineId,
        UUID deviceId,
        UUID modeId,
        String clientVersion,
        String nonce,
        Instant startedAtUtc,
        UUID sessionId
    ) {
        this();
        if (machineId == null) {
            throw new IllegalArgumentException(ErrorMessages.MACHINE_ID);
        }
        if (deviceId == null) {
            throw new IllegalArgumentException(ErrorMessages.DEVICE_ID);
        }
        if (modeId == null) {
            throw new IllegalArgumentException(ErrorMessages.MODE_ID);
        }
        if (clientVersion == null || clientVersion.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.CLIENT_VERSION);
        }
        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.NONCE);
        }

        this.sessionId = sessionId != null ? sessionId : UUID.randomUUID();
        this.machineId = machineId;
        this.deviceId = deviceId;
        this.modeId = modeId;
        this.clientVersion = clientVersion.trim();
        this.nonce = nonce.trim();
        this.startedAtUtc = startedAtUtc != null ? startedAtUtc : Instant.now();
        this.state = GameSessionState.CREATED;
        this.telemetryMode = TelemetryMode.BALL_CHECKPOINT_ONLY;
    }

    public List<GameSessionPlayer> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public List<GameSlot> getSlots() {
        if (this.slots.isEmpty()) {
            for (int i = 1; i <= 4; i++) {
                this.slots.add(new GameSlot(i));
            }
        }
        for (GameSlot slot : this.slots) {
            GameSessionPlayer player = this.players.stream()
                .filter(p -> p.getSlotNumber() == slot.getSlotNumber())
                .findFirst()
                .orElse(null);
            if (player != null && slot.getStatus() != GameSlotStatus.Occupied) {
                slot.status = GameSlotStatus.Occupied;
                slot.userId = player.getUserId();
                slot.guestInitials = player.getGuestInitials();
            } else if (player == null && slot.getStatus() == GameSlotStatus.Occupied) {
                slot.free();
            }

            if (player == null
                && (this.state == GameSessionState.STARTED || this.state == GameSessionState.IN_GAME)
                && slot.getStatus() == GameSlotStatus.Open) {
                slot.lock();
            }
        }
        return Collections.unmodifiableList(this.slots);
    }

    // -------------------- Lobby & Matchmaking --------------------

    public void addPlayer(int slotNumber, UUID userId) {
        addPlayer(slotNumber, userId, null, false);
    }

    public void addPlayer(int slotNumber, UUID userId, String guestInitials, boolean requiresRegisteredUser) {
        if (this.state != GameSessionState.CREATED
            && this.state != GameSessionState.PENDING
            && this.state != GameSessionState.SEARCHING_PLAYER
            && this.state != GameSessionState.JOINED) {
            throw new IllegalStateException(ErrorMessages.ADD_PLAYER_NOT_STARTED);
        }

        if (slotNumber < 1 || slotNumber > 4) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_SLOT_NUMBER);
        }

        if (requiresRegisteredUser && userId == null) {
            throw new IllegalStateException(ErrorMessages.REGISTERED_USER_REQUIRED);
        }

        if (players.size() >= 4) {
            throw new IllegalStateException(ErrorMessages.MAX_PLAYERS_REACHED);
        }

        if (players.stream().anyMatch(p -> p.getSlotNumber() == slotNumber)) {
            throw new IllegalStateException(ErrorMessages.SLOT_ALREADY_OCCUPIED);
        }

        if (userId != null && players.stream().anyMatch(p -> Objects.equals(p.getUserId(), userId))) {
            throw new IllegalStateException(ErrorMessages.USER_ALREADY_REGISTERED);
        }

        getSlots();
        GameSlot targetSlot = this.slots.stream()
            .filter(s -> s.getSlotNumber() == slotNumber)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.INVALID_SLOT_NUMBER));
        targetSlot.occupy(userId, guestInitials);

        GameSessionPlayer player = new GameSessionPlayer(
            this.sessionId,
            slotNumber,
            userId,
            guestInitials,
            Instant.now()
        );
        this.players.add(player);

        if (this.state == GameSessionState.CREATED || this.state == GameSessionState.PENDING) {
            this.state = GameSessionState.JOINED;
        }
    }

    public void removePlayer(int slotNumber) {
        if (this.state != GameSessionState.CREATED
            && this.state != GameSessionState.PENDING
            && this.state != GameSessionState.SEARCHING_PLAYER
            && this.state != GameSessionState.MATCH_FOUND
            && this.state != GameSessionState.JOINED) {
            throw new IllegalStateException(ErrorMessages.CANNOT_REMOVE_PLAYER_ACTIVE_SESSION);
        }

        GameSessionPlayer player = players.stream()
            .filter(p -> p.getSlotNumber() == slotNumber)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(ErrorMessages.PLAYER_NOT_FOUND));

        getSlots();
        GameSlot targetSlot = this.slots.stream()
            .filter(s -> s.getSlotNumber() == slotNumber)
            .findFirst()
            .orElse(null);
        if (targetSlot != null) {
            targetSlot.free();
        }

        this.players.remove(player);

        if (this.state == GameSessionState.MATCH_FOUND) {
            this.state = GameSessionState.PENDING;
        }

        if (this.players.isEmpty() && this.state == GameSessionState.JOINED) {
            this.state = GameSessionState.CREATED;
        }
    }

    public void searchPlayers() {
        if (this.state != GameSessionState.CREATED
            && this.state != GameSessionState.PENDING
            && this.state != GameSessionState.JOINED
            && this.state != GameSessionState.MATCH_FOUND) {
            throw new IllegalStateException(ErrorMessages.CANNOT_SEARCH_PLAYERS);
        }
        this.state = GameSessionState.SEARCHING_PLAYER;
    }

    public void setMatchFound(Instant matchFoundAtUtc) {
        if (this.state != GameSessionState.SEARCHING_PLAYER) {
            throw new IllegalStateException(ErrorMessages.MATCH_FOUND_NOT_SEARCHING);
        }
        if (this.players.size() < 2) {
            throw new IllegalStateException(ErrorMessages.NOT_ENOUGH_PLAYERS_FOR_MATCH);
        }
        this.state = GameSessionState.MATCH_FOUND;
    }

    // -------------------- Session Lifecycle --------------------

    public void launch(Instant launchedAtUtc) {
        if (this.state != GameSessionState.CREATED
            && this.state != GameSessionState.PENDING
            && this.state != GameSessionState.SEARCHING_PLAYER
            && this.state != GameSessionState.JOINED) {
            throw new IllegalStateException(ErrorMessages.CANNOT_LAUNCH_SESSION);
        }

        if (this.players.isEmpty()) {
            throw new IllegalStateException(ErrorMessages.AT_LEAST_ONE_PLAYER_REQUIRED);
        }

        getSlots();
        for (GameSlot slot : this.slots) {
            if (slot.getStatus() == GameSlotStatus.Open) {
                slot.lock();
            }
        }

        this.startedAtUtc = launchedAtUtc != null ? launchedAtUtc : Instant.now();
        this.state = GameSessionState.STARTED;
    }

    public void pause() {
        if (this.state != GameSessionState.STARTED
            && this.state != GameSessionState.IN_GAME
            && this.state != GameSessionState.RESUMED) {
            throw new IllegalStateException(ErrorMessages.PAUSE_SESSION);
        }
        this.state = GameSessionState.PAUSED;
    }

    public void resume() {
        if (this.state != GameSessionState.PAUSED) {
            throw new IllegalStateException(ErrorMessages.RESUME_SESSION);
        }
        this.state = GameSessionState.RESUMED;
    }

    public void endSession(Instant endedAtUtc) {
        if (this.state != GameSessionState.STARTED
            && this.state != GameSessionState.IN_GAME
            && this.state != GameSessionState.RESUMED
            && this.state != GameSessionState.PAUSED) {
            throw new IllegalStateException(ErrorMessages.END_SESSION);
        }
        Instant end = endedAtUtc != null ? endedAtUtc : Instant.now();
        if (end.isBefore(this.startedAtUtc)) {
            throw new IllegalArgumentException(ErrorMessages.EARLIER);
        }
        this.state = GameSessionState.ENDED;
        this.endedAtUtc = end;
    }

    public void abandon(Instant abandonedAtUtc) {
        if (isClosed()) {
            throw new IllegalStateException(ErrorMessages.CLOSED_SESSION);
        }
        Instant end = abandonedAtUtc != null ? abandonedAtUtc : Instant.now();
        if (end.isBefore(this.startedAtUtc)) {
            throw new IllegalArgumentException(ErrorMessages.EARLIER);
        }
        this.state = GameSessionState.ABANDONED;
        this.endedAtUtc = end;
    }

    public void abort(Instant abortedAtUtc) {
        if (isClosed()) {
            throw new IllegalStateException(ErrorMessages.CLOSED_SESSION);
        }
        Instant end = abortedAtUtc != null ? abortedAtUtc : Instant.now();
        if (end.isBefore(this.startedAtUtc)) {
            throw new IllegalArgumentException(ErrorMessages.EARLIER);
        }
        this.state = GameSessionState.ABORTED;
        this.endedAtUtc = end;
    }

    public void cancel() {
        if (isClosed()) {
            throw new IllegalStateException(ErrorMessages.CLOSED_SESSION);
        }
        this.state = GameSessionState.CANCELLED;
        this.endedAtUtc = Instant.now();
    }

    private boolean isClosed() {
        return this.state == GameSessionState.ENDED
            || this.state == GameSessionState.ABANDONED
            || this.state == GameSessionState.ABORTED
            || this.state == GameSessionState.CANCELLED;
    }

    // -------------------- Gameplay & Telemetry --------------------

    public void recordBallScore(int slotNumber, int ballNumber, long score, Instant recordedAtUtc) {
        if (this.state != GameSessionState.STARTED
            && this.state != GameSessionState.IN_GAME
            && this.state != GameSessionState.RESUMED) {
            throw new IllegalStateException(ErrorMessages.CANNOT_RECORD_BALL_INACTIVE_SESSION);
        }

        boolean playerFound = players.stream().anyMatch(p -> p.getSlotNumber() == slotNumber);
        if (!playerFound) {
            throw new IllegalStateException(ErrorMessages.PLAYER_NOT_FOUND);
        }

        if (ballNumber < 1 || ballNumber > 5) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_BALL_NUMBER);
        }

        if (score < 0) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_SCORE);
        }
    }

    public void enableLiveStreaming() {
        if (isClosed()) {
            throw new IllegalStateException(ErrorMessages.CLOSED_SESSION);
        }
        this.telemetryMode = TelemetryMode.LIVE_STREAMING;
    }
}

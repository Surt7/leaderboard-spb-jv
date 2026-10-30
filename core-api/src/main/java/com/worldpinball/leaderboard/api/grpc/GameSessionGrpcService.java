package com.worldpinball.leaderboard.api.grpc;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.AddPlayerHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.AddPlayerResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.commands.AddPlayerCommand;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.GetLobbyHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.GetLobbyResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.dtos.LobbySlotDto;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.dtos.SessionLobbyDto;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.queries.GetLobbyQuery;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer.KickPlayerHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer.KickPlayerResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer.commands.KickPlayerCommand;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession.LaunchSessionHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession.LaunchSessionResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession.commands.LaunchSessionCommand;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore.RecordBallScoreHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore.RecordBallScoreResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore.commands.RecordBallScoreCommand;
import com.worldpinball.leaderboard.application.services.LiveTelemetryService;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.domain.enums.GameSlotStatus;
import com.worldpinball.leaderboard.grpc.v1.AddPlayerRequest;
import com.worldpinball.leaderboard.grpc.v1.AddPlayerResponse;
import com.worldpinball.leaderboard.grpc.v1.GameSessionServiceGrpc;
import com.worldpinball.leaderboard.grpc.v1.GetLobbyRequest;
import com.worldpinball.leaderboard.grpc.v1.KickPlayerRequest;
import com.worldpinball.leaderboard.grpc.v1.KickPlayerResponse;
import com.worldpinball.leaderboard.grpc.v1.LaunchSessionRequest;
import com.worldpinball.leaderboard.grpc.v1.LaunchSessionResponse;
import com.worldpinball.leaderboard.grpc.v1.PlayerSlotInfo;
import com.worldpinball.leaderboard.grpc.v1.RecordBallScoreRequest;
import com.worldpinball.leaderboard.grpc.v1.RecordBallScoreResponse;
import com.worldpinball.leaderboard.grpc.v1.ScoreDeltaRequest;
import com.worldpinball.leaderboard.grpc.v1.SessionLobbyResponse;
import com.worldpinball.leaderboard.grpc.v1.SlotStatus;
import com.worldpinball.leaderboard.grpc.v1.StreamingSummaryResponse;
import com.worldpinball.leaderboard.grpc.v1.WatchAllLobbiesRequest;
import com.worldpinball.leaderboard.grpc.v1.WatchLobbyRequest;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class GameSessionGrpcService extends GameSessionServiceGrpc.GameSessionServiceImplBase {

    private static final Set<String> TERMINAL_STATES = Set.of(
        "ENDED", "CLOSED", "CANCELLED", "ABANDONED", "ABORTED"
    );

    private final AddPlayerHandler addPlayerHandler;
    private final KickPlayerHandler kickPlayerHandler;
    private final GetLobbyHandler getLobbyHandler;
    private final RecordBallScoreHandler recordBallScoreHandler;
    private final LaunchSessionHandler launchSessionHandler;
    private final LobbyNotificationService lobbyNotifier;
    private final LiveTelemetryService liveTelemetryService;
    private final GameSessionRepository gameSessionRepository;

    @Override
    public void addPlayer(AddPlayerRequest request, StreamObserver<AddPlayerResponse> responseObserver) {
        UUID sessionId;
        try {
            sessionId = UUID.fromString(request.getSessionId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid session ID format").asRuntimeException());
            return;
        }

        UUID userId = null;
        if (request.hasUserId() && !request.getUserId().isBlank()) {
            try {
                userId = UUID.fromString(request.getUserId());
            } catch (IllegalArgumentException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid user ID format").asRuntimeException());
                return;
            }
        }

        String initials = request.hasGuestInitials() ? request.getGuestInitials() : null;
        var command = new AddPlayerCommand(sessionId, request.getSlotNumber(), userId, initials, false);
        AddPlayerResult result = addPlayerHandler.handle(command);

        switch (result) {
            case AddPlayerResult.Success(UUID sid, int slotNumber) -> {
                lobbyNotifier.notifyLobbyUpdated(sessionId);
                log.info("Added player to slot {} for session {}.", command.slotNumber(), command.sessionId());
                responseObserver.onNext(AddPlayerResponse.newBuilder()
                    .setSuccess(true)
                    .setSlotNumber(slotNumber)
                    .setMessage("Player added.")
                    .build());
                responseObserver.onCompleted();
            }
            case AddPlayerResult.SessionNotFound(UUID sid) ->
                responseObserver.onError(Status.NOT_FOUND.withDescription("Session not found.").asRuntimeException());
            case AddPlayerResult.SessionNotJoinable(UUID sid) ->
                responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("Session is not joinable.").asRuntimeException());
            case AddPlayerResult.SlotAlreadyOccupied(int slotNumber) ->
                responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Slot is already occupied.").asRuntimeException());
            case AddPlayerResult.InvalidSlot(int slotNumber) ->
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Slot number must be between 1 and 4.").asRuntimeException());
            case AddPlayerResult.RegisteredUserRequired(UUID sid) ->
                responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Registered user required.").asRuntimeException());
            case AddPlayerResult.UserAlreadyRegistered(UUID uid) ->
                responseObserver.onError(Status.ALREADY_EXISTS.withDescription("User already registered.").asRuntimeException());
        }
    }

    @Override
    public void kickPlayer(KickPlayerRequest request, StreamObserver<KickPlayerResponse> responseObserver) {
        UUID sessionId;
        try {
            sessionId = UUID.fromString(request.getSessionId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid session ID format").asRuntimeException());
            return;
        }

        var command = new KickPlayerCommand(sessionId, request.getSlotNumber());
        KickPlayerResult result = kickPlayerHandler.handle(command);

        switch (result) {
            case KickPlayerResult.Success(UUID sid, int slotNumber) -> {
                lobbyNotifier.notifyLobbyUpdated(sessionId);
                responseObserver.onNext(KickPlayerResponse.newBuilder()
                    .setSuccess(true)
                    .setSlotNumber(slotNumber)
                    .setMessage("Player kicked.")
                    .build());
                responseObserver.onCompleted();
            }
            case KickPlayerResult.SessionNotFound(UUID sid) ->
                responseObserver.onError(Status.NOT_FOUND.withDescription("Session not found.").asRuntimeException());
            case KickPlayerResult.CannotRemovePlayerActiveSession(UUID sid) ->
                responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("Cannot remove player once session has started or ended.").asRuntimeException());
            case KickPlayerResult.PlayerNotFound(int slotNumber) ->
                responseObserver.onError(Status.NOT_FOUND.withDescription("Player not found in the specified slot.").asRuntimeException());
            case KickPlayerResult.InvalidSlot(int slotNumber) ->
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Slot number must be between 1 and 4.").asRuntimeException());
        }
    }

    @Override
    public void launchSession(LaunchSessionRequest request, StreamObserver<LaunchSessionResponse> responseObserver) {
        UUID sessionId;
        try {
            sessionId = UUID.fromString(request.getSessionId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid session ID format").asRuntimeException());
            return;
        }

        var command = new LaunchSessionCommand(sessionId);
        LaunchSessionResult result = launchSessionHandler.handle(command);

        switch (result) {
            case LaunchSessionResult.Success(UUID id) -> {
                lobbyNotifier.notifyLobbyUpdated(sessionId);
                responseObserver.onNext(LaunchSessionResponse.newBuilder()
                        .setSuccess(true)
                        .setSessionId(id.toString())
                        .setMessage("Session launched.")
                        .build());
                responseObserver.onCompleted();
            }
            case LaunchSessionResult.SessionNotFound(UUID sid) ->
                    responseObserver.onError(Status.NOT_FOUND.withDescription("Session not found.").asRuntimeException());
            case LaunchSessionResult.CannotStartSession(String message) ->
                    responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(message).asRuntimeException());
        }
    }

    @Override
    public void getLobby(GetLobbyRequest request, StreamObserver<SessionLobbyResponse> responseObserver) {
        UUID sessionId;
        try {
            sessionId = UUID.fromString(request.getSessionId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid session ID format").asRuntimeException());
            return;
        }

        var query = new GetLobbyQuery(sessionId);
        GetLobbyResult result = getLobbyHandler.handle(query);

        switch (result) {
            case GetLobbyResult.Success(SessionLobbyDto lobby) -> {
                responseObserver.onNext(mapToLobbyResponse(lobby));
                responseObserver.onCompleted();
            }
            case GetLobbyResult.SessionNotFound(UUID sid) ->
                responseObserver.onError(Status.NOT_FOUND.withDescription("Session not found.").asRuntimeException());
        }
    }

    @Override
    public void watchLobby(WatchLobbyRequest request, StreamObserver<SessionLobbyResponse> responseObserver) {
        UUID sessionId;
        try {
            sessionId = UUID.fromString(request.getSessionId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid session ID format").asRuntimeException());
            return;
        }

        // Send initial snapshot
        GetLobbyResult initialResult = getLobbyHandler.handle(new GetLobbyQuery(sessionId));
        if (!(initialResult instanceof GetLobbyResult.Success(SessionLobbyDto lobby1))) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Session not found.").asRuntimeException());
            return;
        }

        responseObserver.onNext(mapToLobbyResponse(lobby1));

        if (isTerminal(lobby1.sessionState())) {
            responseObserver.onCompleted();
            return;
        }

        ServerCallStreamObserver<SessionLobbyResponse> serverObserver = (ServerCallStreamObserver<SessionLobbyResponse>) responseObserver;
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Runnable> unsubscribeRef = new AtomicReference<>();

        Runnable unsub = lobbyNotifier.subscribe(sessionId, sid -> {
            if (completed.get()) {
                return;
            }
            GetLobbyResult update = getLobbyHandler.handle(new GetLobbyQuery(sid));
            switch (update) {
                case GetLobbyResult.Success(SessionLobbyDto lobby) -> {
                    synchronized (responseObserver) {
                        if (!completed.get()) {
                            responseObserver.onNext(mapToLobbyResponse(lobby));
                            if (isTerminal(lobby.sessionState())) {
                                completed.set(true);
                                Runnable r = unsubscribeRef.get();
                                if (r != null) {
                                    r.run();
                                }
                                responseObserver.onCompleted();
                            }
                        }
                    }
                }
                case GetLobbyResult.SessionNotFound ignored -> {
                    // Session no longer found during notification cycle: nothing to push
                }
            }
        });

        unsubscribeRef.set(unsub);
        serverObserver.setOnCancelHandler(() -> {
            completed.set(true);
            Runnable r = unsubscribeRef.get();
            if (r != null) {
                r.run();
            }
        });
    }

    @Override
    public void watchAllLobbies(WatchAllLobbiesRequest request, StreamObserver<SessionLobbyResponse> responseObserver) {
        log.info("watchAllLobbies: Client subscribed to global stream");

        ServerCallStreamObserver<SessionLobbyResponse> serverObserver = (ServerCallStreamObserver<SessionLobbyResponse>) responseObserver;
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Runnable> unsubscribeRef = new AtomicReference<>();
        Map<String, String> lastFingerprints = new ConcurrentHashMap<>();

        // 1. Subscribe before initial snapshot to prevent race conditions
        Runnable unsub = lobbyNotifier.subscribeAll(sid -> {
            if (completed.get()) {
                return;
            }
            log.info("watchAllLobbies: notification received for session {}", sid);
            GetLobbyResult update = getLobbyHandler.handle(new GetLobbyQuery(sid));
            if (update instanceof GetLobbyResult.Success(SessionLobbyDto lobby)) {
                synchronized (responseObserver) {
                    if (!completed.get()) {
                        sendIfNotDuplicate(lobby, lastFingerprints, responseObserver);
                    }
                }
            } else {
                log.warn("watchAllLobbies: could not resolve lobby for session {}: {}", sid, update);
            }
        });
        unsubscribeRef.set(unsub);

        serverObserver.setOnCancelHandler(() -> {
            completed.set(true);
            Runnable r = unsubscribeRef.get();
            if (r != null) {
                r.run();
            }
        });

        // 2. Initial snapshot: send all currently active sessions
        List<UUID> activeSessionIds = gameSessionRepository.findActiveSessionIds();
        for (UUID activeId : activeSessionIds) {
            if (completed.get()) {
                break;
            }
            GetLobbyResult initial = getLobbyHandler.handle(new GetLobbyQuery(activeId));
            if (initial instanceof GetLobbyResult.Success(SessionLobbyDto lobby)) {
                synchronized (responseObserver) {
                    if (!completed.get()) {
                        sendIfNotDuplicate(lobby, lastFingerprints, responseObserver);
                    }
                }
            }
        }
    }

    private static void sendIfNotDuplicate(
            SessionLobbyDto lobby,
            Map<String, String> lastFingerprints,
            StreamObserver<SessionLobbyResponse> responseObserver
    ) {
        SessionLobbyResponse response = mapToLobbyResponse(lobby);
        String slotsSig = response.getSlotsList().stream()
                .map(s -> s.getSlotNumber() + ":" + s.getStatus() + ":" + s.getUserId())
                .collect(Collectors.joining(","));
        String fingerprint = response.getSessionState() + "_" + slotsSig;

        String previous = lastFingerprints.put(response.getSessionId(), fingerprint);
        if (!fingerprint.equals(previous)) {
            responseObserver.onNext(response);
        }
    }

    @Override
    public void recordBallScore(RecordBallScoreRequest request, StreamObserver<RecordBallScoreResponse> responseObserver) {
        UUID sessionId;
        UUID deviceId;
        try {
            sessionId = UUID.fromString(request.getSessionId());
            deviceId = UUID.fromString(request.getDeviceId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid SessionId or DeviceId format.").asRuntimeException());
            return;
        }

        var command = new RecordBallScoreCommand(
            sessionId,
            deviceId,
            request.getSlotNumber(),
            request.getBallNumber(),
            request.getScore()
        );

        RecordBallScoreResult result = recordBallScoreHandler.handle(command);

        switch (result) {
            case RecordBallScoreResult.Success(UUID sid, int slotNumber, int ballNumber, long score) -> {
                lobbyNotifier.notifyLobbyUpdated(sessionId);
                responseObserver.onNext(RecordBallScoreResponse.newBuilder()
                    .setSuccess(true)
                    .setSessionId(sid.toString())
                    .setSlotNumber(slotNumber)
                    .setBallNumber(ballNumber)
                    .setScore(score)
                    .setMessage("Ball score recorded.")
                    .build());
                responseObserver.onCompleted();
            }
            case RecordBallScoreResult.SessionNotFound(UUID sid) ->
                responseObserver.onError(Status.NOT_FOUND.withDescription("Session not found.").asRuntimeException());
            case RecordBallScoreResult.DeviceMismatch(UUID expected, UUID actual) ->
                responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Device mismatch.").asRuntimeException());
            case RecordBallScoreResult.CannotRecordBallScore(String reason) ->
                responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(reason).asRuntimeException());
        }
    }

    @Override
    public StreamObserver<ScoreDeltaRequest> streamLiveTelemetry(StreamObserver<StreamingSummaryResponse> responseObserver) {
        return new StreamObserver<>() {
            private long totalScore = 0;
            private int eventsProcessed = 0;
            private UUID sessionId = null;
            private int slotNumber = 0;

            @Override
            public void onNext(ScoreDeltaRequest delta) {
                try {
                    sessionId = UUID.fromString(delta.getSessionId());
                } catch (IllegalArgumentException e) {
                    responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid SessionId format.").asRuntimeException());
                    return;
                }
                slotNumber = delta.getSlotNumber();
                totalScore = liveTelemetryService.ingestDelta(
                    sessionId,
                    delta.getSlotNumber(),
                    delta.getPointsDelta(),
                    delta.getTriggerEvent()
                );
                eventsProcessed++;
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Telemetry stream error: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(StreamingSummaryResponse.newBuilder()
                    .setSuccess(true)
                    .setSessionId(sessionId != null ? sessionId.toString() : "")
                    .setSlotNumber(slotNumber)
                    .setTotalStreamedScore(totalScore)
                    .setTotalEventsProcessed(eventsProcessed)
                    .setMessage("Live telemetry stream completed successfully.")
                    .build());
                responseObserver.onCompleted();
            }
        };
    }

    private static SessionLobbyResponse mapToLobbyResponse(SessionLobbyDto lobby) {
        var responseBuilder = SessionLobbyResponse.newBuilder()
            .setSessionId(lobby.sessionId().toString())
            .setSessionState(lobby.sessionState());

        for (LobbySlotDto slot : lobby.slots()) {
            SlotStatus protoStatus = mapSlotStatus(slot.status());
            var slotBuilder = PlayerSlotInfo.newBuilder()
                .setSlotNumber(slot.slotNumber())
                .setIsOccupied(slot.isOccupied())
                .setStatus(protoStatus);

            if (slot.userId() != null) {
                slotBuilder.setUserId(slot.userId().toString());
            }
            if (slot.guestInitials() != null && !slot.guestInitials().isBlank()) {
                slotBuilder.setGuestInitials(slot.guestInitials());
            }
            responseBuilder.addSlots(slotBuilder.build());
        }

        return responseBuilder.build();
    }

    private static SlotStatus mapSlotStatus(GameSlotStatus status) {
        if (status == null) {
            return SlotStatus.SLOT_STATUS_OPEN;
        }
        return switch (status) {
            case Open -> SlotStatus.SLOT_STATUS_OPEN;
            case Occupied -> SlotStatus.SLOT_STATUS_OCCUPIED;
            case Locked -> SlotStatus.SLOT_STATUS_LOCKED;
        };
    }

    private static boolean isTerminal(String sessionState) {
        return sessionState != null && TERMINAL_STATES.contains(sessionState.toUpperCase());
    }
}

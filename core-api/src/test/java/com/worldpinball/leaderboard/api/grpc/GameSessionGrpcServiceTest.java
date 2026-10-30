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
import com.worldpinball.leaderboard.grpc.v1.AddPlayerRequest;
import com.worldpinball.leaderboard.grpc.v1.AddPlayerResponse;
import com.worldpinball.leaderboard.grpc.v1.GetLobbyRequest;
import com.worldpinball.leaderboard.grpc.v1.KickPlayerRequest;
import com.worldpinball.leaderboard.grpc.v1.KickPlayerResponse;
import com.worldpinball.leaderboard.grpc.v1.LaunchSessionRequest;
import com.worldpinball.leaderboard.grpc.v1.LaunchSessionResponse;
import com.worldpinball.leaderboard.grpc.v1.RecordBallScoreRequest;
import com.worldpinball.leaderboard.grpc.v1.RecordBallScoreResponse;
import com.worldpinball.leaderboard.grpc.v1.ScoreDeltaRequest;
import com.worldpinball.leaderboard.grpc.v1.SessionLobbyResponse;
import com.worldpinball.leaderboard.grpc.v1.StreamingSummaryResponse;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameSessionGrpcServiceTest {

    @Mock
    private AddPlayerHandler addPlayerHandler;

    @Mock
    private KickPlayerHandler kickPlayerHandler;

    @Mock
    private GetLobbyHandler getLobbyHandler;

    @Mock
    private RecordBallScoreHandler recordBallScoreHandler;

    @Mock
    private LaunchSessionHandler launchSessionHandler;

    @Mock
    private LobbyNotificationService lobbyNotifier;

    @Mock
    private LiveTelemetryService liveTelemetryService;

    @Mock
    private GameSessionRepository gameSessionRepository;

    private GameSessionGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new GameSessionGrpcService(
            addPlayerHandler,
            kickPlayerHandler,
            getLobbyHandler,
            recordBallScoreHandler,
            launchSessionHandler,
            lobbyNotifier,
            liveTelemetryService,
            gameSessionRepository
        );
    }

    @Test
    void addPlayer_shouldSucceed_whenValidRequest() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AddPlayerRequest request = AddPlayerRequest.newBuilder()
            .setSessionId(sessionId.toString())
            .setSlotNumber(1)
            .setUserId(userId.toString())
            .setGuestInitials("AAA")
            .build();

        when(addPlayerHandler.handle(any(AddPlayerCommand.class)))
            .thenReturn(new AddPlayerResult.Success(sessionId, 1));

        @SuppressWarnings("unchecked")
        StreamObserver<AddPlayerResponse> observer = mock(StreamObserver.class);

        grpcService.addPlayer(request, observer);

        verify(lobbyNotifier).notifyLobbyUpdated(sessionId);
        ArgumentCaptor<AddPlayerResponse> captor = ArgumentCaptor.forClass(AddPlayerResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();

        assertThat(captor.getValue().getSuccess()).isTrue();
        assertThat(captor.getValue().getSlotNumber()).isEqualTo(1);
    }

    @Test
    void addPlayer_shouldReturnInvalidArgument_whenSessionIdIsInvalid() {
        AddPlayerRequest request = AddPlayerRequest.newBuilder()
            .setSessionId("invalid-guid")
            .build();

        @SuppressWarnings("unchecked")
        StreamObserver<AddPlayerResponse> observer = mock(StreamObserver.class);

        grpcService.addPlayer(request, observer);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) captor.getValue()).getStatus().getCode())
            .isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void kickPlayer_shouldSucceed_whenPlayerExists() {
        UUID sessionId = UUID.randomUUID();
        KickPlayerRequest request = KickPlayerRequest.newBuilder()
            .setSessionId(sessionId.toString())
            .setSlotNumber(2)
            .build();

        when(kickPlayerHandler.handle(any(KickPlayerCommand.class)))
            .thenReturn(new KickPlayerResult.Success(sessionId, 2));

        @SuppressWarnings("unchecked")
        StreamObserver<KickPlayerResponse> observer = mock(StreamObserver.class);

        grpcService.kickPlayer(request, observer);

        verify(lobbyNotifier).notifyLobbyUpdated(sessionId);
        ArgumentCaptor<KickPlayerResponse> captor = ArgumentCaptor.forClass(KickPlayerResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();

        assertThat(captor.getValue().getSuccess()).isTrue();
        assertThat(captor.getValue().getSlotNumber()).isEqualTo(2);
    }

    @Test
    void launchSession_shouldSucceed_whenValid() {
        UUID sessionId = UUID.randomUUID();
        LaunchSessionRequest request = LaunchSessionRequest.newBuilder()
            .setSessionId(sessionId.toString())
            .build();

        when(launchSessionHandler.handle(any(LaunchSessionCommand.class)))
            .thenReturn(new LaunchSessionResult.Success(sessionId));

        @SuppressWarnings("unchecked")
        StreamObserver<LaunchSessionResponse> observer = mock(StreamObserver.class);

        grpcService.launchSession(request, observer);

        verify(lobbyNotifier).notifyLobbyUpdated(sessionId);
        ArgumentCaptor<LaunchSessionResponse> captor = ArgumentCaptor.forClass(LaunchSessionResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();

        assertThat(captor.getValue().getSuccess()).isTrue();
        assertThat(captor.getValue().getSessionId()).isEqualTo(sessionId.toString());
    }

    @Test
    void getLobby_shouldReturnLobby_whenFound() {
        UUID sessionId = UUID.randomUUID();
        GetLobbyRequest request = GetLobbyRequest.newBuilder()
            .setSessionId(sessionId.toString())
            .build();

        SessionLobbyDto dto = new SessionLobbyDto(
            sessionId,
            "WAITING",
            List.of(new LobbySlotDto(1, true, UUID.randomUUID(), "AAA"))
        );

        when(getLobbyHandler.handle(any(GetLobbyQuery.class)))
            .thenReturn(new GetLobbyResult.Success(dto));

        @SuppressWarnings("unchecked")
        StreamObserver<SessionLobbyResponse> observer = mock(StreamObserver.class);

        grpcService.getLobby(request, observer);

        ArgumentCaptor<SessionLobbyResponse> captor = ArgumentCaptor.forClass(SessionLobbyResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();

        assertThat(captor.getValue().getSessionId()).isEqualTo(sessionId.toString());
        assertThat(captor.getValue().getSessionState()).isEqualTo("WAITING");
        assertThat(captor.getValue().getSlotsCount()).isEqualTo(1);
    }

    @Test
    void recordBallScore_shouldSucceed_whenValid() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        RecordBallScoreRequest request = RecordBallScoreRequest.newBuilder()
            .setSessionId(sessionId.toString())
            .setDeviceId(deviceId.toString())
            .setSlotNumber(1)
            .setBallNumber(1)
            .setScore(500000L)
            .build();

        when(recordBallScoreHandler.handle(any(RecordBallScoreCommand.class)))
            .thenReturn(new RecordBallScoreResult.Success(sessionId, 1, 1, 500000L));

        @SuppressWarnings("unchecked")
        StreamObserver<RecordBallScoreResponse> observer = mock(StreamObserver.class);

        grpcService.recordBallScore(request, observer);

        verify(lobbyNotifier).notifyLobbyUpdated(sessionId);
        ArgumentCaptor<RecordBallScoreResponse> captor = ArgumentCaptor.forClass(RecordBallScoreResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();

        assertThat(captor.getValue().getSuccess()).isTrue();
        assertThat(captor.getValue().getScore()).isEqualTo(500000L);
    }

    @Test
    void streamLiveTelemetry_shouldProcessDeltasAndReturnSummary() {
        UUID sessionId = UUID.randomUUID();
        when(liveTelemetryService.ingestDelta(eq(sessionId), eq(1), eq(1000L), any()))
            .thenReturn(1000L);
        when(liveTelemetryService.ingestDelta(eq(sessionId), eq(1), eq(2500L), any()))
            .thenReturn(3500L);

        @SuppressWarnings("unchecked")
        StreamObserver<StreamingSummaryResponse> responseObserver = mock(StreamObserver.class);

        StreamObserver<ScoreDeltaRequest> requestObserver = grpcService.streamLiveTelemetry(responseObserver);

        requestObserver.onNext(ScoreDeltaRequest.newBuilder()
            .setSessionId(sessionId.toString())
            .setSlotNumber(1)
            .setPointsDelta(1000L)
            .setTriggerEvent("BumperHit")
            .build());

        requestObserver.onNext(ScoreDeltaRequest.newBuilder()
            .setSessionId(sessionId.toString())
            .setSlotNumber(1)
            .setPointsDelta(2500L)
            .setTriggerEvent("RampComplete")
            .build());

        requestObserver.onCompleted();

        ArgumentCaptor<StreamingSummaryResponse> captor = ArgumentCaptor.forClass(StreamingSummaryResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        StreamingSummaryResponse summary = captor.getValue();
        assertThat(summary.getSuccess()).isTrue();
        assertThat(summary.getTotalEventsProcessed()).isEqualTo(2);
        assertThat(summary.getTotalStreamedScore()).isEqualTo(3500L);
    }
}

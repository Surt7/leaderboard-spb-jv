package com.worldpinball.leaderboard.api.controllers.Public;

import com.worldpinball.leaderboard.api.dtos.SessionContracts.*;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession.AbortSessionHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession.AbortSessionResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession.commands.AbortSessionCommand;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.AddPlayerHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.AddPlayerResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.commands.AddPlayerCommand;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession.CreateGameSessionHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession.CreateGameSessionResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession.commands.CreateGameSessionCommand;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.GetLobbyHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.GetLobbyResult;
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
import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.SubmitScoreHandler;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionsController Tests")
class SessionsControllerTest {

    @Mock
    private CreateGameSessionHandler createGameSessionHandler;

    @Mock
    private GetLobbyHandler getLobbyHandler;

    @Mock
    private AddPlayerHandler addPlayerHandler;

    @Mock
    private KickPlayerHandler kickPlayerHandler;

    @Mock
    private LaunchSessionHandler launchSessionHandler;

    @Mock
    private AbortSessionHandler abortSessionHandler;

    @Mock
    private RecordBallScoreHandler recordBallScoreHandler;

    @Mock
    private SubmitScoreHandler submitScoreHandler;

    @Mock
    private LobbyNotificationService lobbyNotifier;

    @Mock
    private com.worldpinball.leaderboard.infrastructure.security.cryptography.MachineAuthService machineAuthService;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private SessionsController sut;

    @BeforeEach
    void setUp() {
        sut = new SessionsController(
                createGameSessionHandler,
                getLobbyHandler,
                addPlayerHandler,
                kickPlayerHandler,
                launchSessionHandler,
                abortSessionHandler,
                recordBallScoreHandler,
                submitScoreHandler,
                lobbyNotifier,
                machineAuthService,
                objectMapper
        );
    }

    @Test
    @DisplayName("CreateGameSession returns 200 OK with session details when valid")
    void createGameSession_whenValid_shouldReturnOk() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Instant now = Instant.now();

        when(createGameSessionHandler.handle(any(CreateGameSessionCommand.class)))
                .thenReturn(new CreateGameSessionResult.Success(sessionId, "nonce-123", now));

        CreateGameSessionRequest request = new CreateGameSessionRequest(deviceId, "classic", "1.0.0");
        ResponseEntity<?> response = sut.createGameSession(request, null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CreateGameSessionResponse body = (CreateGameSessionResponse) response.getBody();
        assertThat(body.sessionId()).isEqualTo(sessionId);
        assertThat(body.nonce()).isEqualTo("nonce-123");
        verify(lobbyNotifier).notifyLobbyUpdated(sessionId);
    }

    @Test
    @DisplayName("CreateGameSession with machine signature serializes modeCode and clientVersion without deviceId")
    void createGameSession_withMachineSignature_serializesExpectedCanonicalBody() {
        UUID sessionId = UUID.randomUUID();
        UUID machineId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Instant now = Instant.now();

        jakarta.servlet.http.HttpServletRequest httpRequest = org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        when(httpRequest.getMethod()).thenReturn("POST");
        when(httpRequest.getRequestURI()).thenReturn("/api/v1/sessions/create");

        when(machineAuthService.validate(any(com.worldpinball.leaderboard.application.security.cryptography.MachineAuthContext.class)))
                .thenAnswer(invocation -> {
                    com.worldpinball.leaderboard.application.security.cryptography.MachineAuthContext ctx = invocation.getArgument(0);
                    assertThat(ctx.rawBody()).isEqualTo("{\"modeCode\":\"classic\",\"clientVersion\":\"1.0.0\"}");
                    return new com.worldpinball.leaderboard.application.security.cryptography.MachineAuthResult.Success(
                            machineId, deviceId, "nonce-1234567890123456", 1758110400L
                    );
                });

        when(createGameSessionHandler.handle(any(CreateGameSessionCommand.class)))
                .thenReturn(new CreateGameSessionResult.Success(sessionId, "nonce-123", now));

        CreateGameSessionRequest request = new CreateGameSessionRequest("classic", "1.0.0");
        ResponseEntity<?> response = sut.createGameSession(
                request,
                deviceId.toString(),
                "1758110400",
                "nonce-1234567890123456",
                "mock-sig",
                httpRequest
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CreateGameSessionResponse body = (CreateGameSessionResponse) response.getBody();
        assertThat(body.sessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("GetLobby returns 200 OK when session exists")
    void getLobby_whenSessionExists_shouldReturnOk() {
        UUID sessionId = UUID.randomUUID();
        SessionLobbyDto lobbyDto = new SessionLobbyDto(sessionId, "Created", List.of());

        when(getLobbyHandler.handle(any(GetLobbyQuery.class)))
                .thenReturn(new GetLobbyResult.Success(lobbyDto));

        ResponseEntity<?> response = sut.getLobby(sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(lobbyDto);
    }

    @Test
    @DisplayName("AddPlayer returns 200 OK when request is valid")
    void addPlayer_whenRequestIsValid_shouldReturnOk() {
        UUID sessionId = UUID.randomUUID();
        when(addPlayerHandler.handle(any(AddPlayerCommand.class)))
                .thenReturn(new AddPlayerResult.Success(sessionId, 1));

        AddPlayerRequest request = new AddPlayerRequest(1, null, "AAA", false);
        ResponseEntity<?> response = sut.addPlayer(sessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AddPlayerResponse body = (AddPlayerResponse) response.getBody();
        assertThat(body.sessionId()).isEqualTo(sessionId);
        assertThat(body.slotNumber()).isEqualTo(1);
        verify(lobbyNotifier).notifyLobbyUpdated(sessionId);
    }

    @Test
    @DisplayName("KickPlayer returns 200 OK when player exists")
    void kickPlayer_whenPlayerExists_shouldReturnOk() {
        UUID sessionId = UUID.randomUUID();
        when(kickPlayerHandler.handle(any(KickPlayerCommand.class)))
                .thenReturn(new KickPlayerResult.Success(sessionId, 2));

        ResponseEntity<?> response = sut.kickPlayer(sessionId, 2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        KickPlayerResponse body = (KickPlayerResponse) response.getBody();
        assertThat(body.sessionId()).isEqualTo(sessionId);
        assertThat(body.slotNumber()).isEqualTo(2);
        verify(lobbyNotifier).notifyLobbyUpdated(sessionId);
    }

    @Test
    @DisplayName("LaunchSession returns 200 OK when session is ready")
    void launchSession_whenSessionReady_shouldReturnOk() {
        UUID sessionId = UUID.randomUUID();
        when(launchSessionHandler.handle(any(LaunchSessionCommand.class)))
                .thenReturn(new LaunchSessionResult.Success(sessionId));

        ResponseEntity<?> response = sut.launchSession(sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LaunchSessionResponse body = (LaunchSessionResponse) response.getBody();
        assertThat(body.sessionId()).isEqualTo(sessionId);
        verify(lobbyNotifier).notifyLobbyUpdated(sessionId);
    }

    @Test
    @DisplayName("AbortSession returns 200 OK when session is aborted")
    void abortSession_whenSessionAborted_shouldReturnOk() {
        UUID sessionId = UUID.randomUUID();
        when(abortSessionHandler.handle(any(AbortSessionCommand.class)))
                .thenReturn(new AbortSessionResult.Success(sessionId));

        ResponseEntity<?> response = sut.abortSession(sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AbortSessionResponse body = (AbortSessionResponse) response.getBody();
        assertThat(body.sessionId()).isEqualTo(sessionId);
        verify(lobbyNotifier).notifyLobbyUpdated(sessionId);
    }

    @Test
    @DisplayName("RecordBallScore returns 200 OK when ball score recorded")
    void recordBallScore_whenValid_shouldReturnOk() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();

        when(recordBallScoreHandler.handle(any(RecordBallScoreCommand.class)))
                .thenReturn(new RecordBallScoreResult.Success(sessionId, 1, 1, 150_000L));

        RecordBallScoreRequest request = new RecordBallScoreRequest(deviceId, 1, 1, 150_000L);
        ResponseEntity<?> response = sut.recordBallScore(sessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        RecordBallScoreResponse body = (RecordBallScoreResponse) response.getBody();
        assertThat(body.sessionId()).isEqualTo(sessionId);
        assertThat(body.score()).isEqualTo(150_000L);
        verify(lobbyNotifier).notifyLobbyUpdated(sessionId);
    }
}

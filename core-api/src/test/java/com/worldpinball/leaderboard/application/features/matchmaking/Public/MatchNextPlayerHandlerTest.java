package com.worldpinball.leaderboard.application.features.matchmaking.Public;

import com.worldpinball.leaderboard.application.features.matchmaking.Public.matchnextplayer.MatchNextPlayerHandlerImpl;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.matchnextplayer.MatchNextPlayerResult;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.matchnextplayer.MatchNextPlayerCommand;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.application.services.MatchmakingQueueService;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.domain.enums.GameSessionState;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchNextPlayerHandler Tests")
class MatchNextPlayerHandlerTest {

    @Mock
    private GameSessionRepository sessionRepository;

    @Mock
    private MatchmakingQueueService queueService;

    @Mock
    private LobbyNotificationService lobbyNotificationService;

    private MatchNextPlayerHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new MatchNextPlayerHandlerImpl(sessionRepository, queueService, lobbyNotificationService);
    }

    @Test
    @DisplayName("Returns SessionNotFound when game session does not exist")
    void handle_whenSessionDoesNotExist_shouldReturnSessionNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        MatchNextPlayerCommand command = new MatchNextPlayerCommand(sessionId, UUID.randomUUID());
        MatchNextPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(MatchNextPlayerResult.SessionNotFound.class);
        MatchNextPlayerResult.SessionNotFound notFound = (MatchNextPlayerResult.SessionNotFound) result;
        assertThat(notFound.sessionId()).isEqualTo(sessionId);

        verifyNoInteractions(queueService);
    }

    @Test
    @DisplayName("Returns NoPlayerInQueue when queue from machine has no waiting players")
    void handle_whenQueueIsEmpty_shouldReturnNoPlayerInQueue() {
        UUID machineId = UUID.randomUUID();
        GameSession session = new GameSession(
                machineId, UUID.randomUUID(), UUID.randomUUID(), "1.0", "nonce", Instant.now(), null
        );
        when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
        when(queueService.dequeueNext(machineId.toString())).thenReturn(Optional.empty());

        MatchNextPlayerCommand command = new MatchNextPlayerCommand(session.getSessionId(), machineId);
        MatchNextPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(MatchNextPlayerResult.NoPlayerInQueue.class);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Returns SessionFull when session already has 4 players")
    void handle_whenSessionIsFull_shouldReturnSessionFull() {
        UUID machineId = UUID.randomUUID();
        GameSession session = new GameSession(
                machineId, UUID.randomUUID(), UUID.randomUUID(), "1.0", "nonce", Instant.now(), null
        );
        session.addPlayer(1, UUID.randomUUID());
        session.addPlayer(2, UUID.randomUUID());
        session.addPlayer(3, UUID.randomUUID());
        session.addPlayer(4, UUID.randomUUID());
        assertThat(session.getPlayers()).hasSize(4);

        when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));

        MatchNextPlayerCommand command = new MatchNextPlayerCommand(session.getSessionId(), machineId);
        MatchNextPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(MatchNextPlayerResult.SessionFull.class);
        verifyNoInteractions(queueService);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dequeues waiting player, adds to next slot, saves session and returns Success")
    void handle_whenQueueHasPlayer_shouldReturnSuccess() {
        UUID machineId = UUID.randomUUID();
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();

        GameSession session = new GameSession(
                machineId, UUID.randomUUID(), UUID.randomUUID(), "1.0", "nonce", Instant.now(), null
        );
        session.addPlayer(1, player1Id);

        when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
        when(queueService.dequeueNext(machineId.toString())).thenReturn(Optional.of(player2Id));

        MatchNextPlayerCommand command = new MatchNextPlayerCommand(session.getSessionId(), machineId);
        MatchNextPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(MatchNextPlayerResult.Success.class);
        MatchNextPlayerResult.Success success = (MatchNextPlayerResult.Success) result;
        assertThat(success.playerId()).isEqualTo(player2Id);
        assertThat(success.slotNumber()).isEqualTo(2);

        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("When session is searching for players and reaches 2 players, marks MatchFound and notifies lobby")
    void handle_whenMatchFoundState_shouldNotifyMatchFound() {
        UUID machineId = UUID.randomUUID();
        GameSession session = new GameSession(
                machineId, UUID.randomUUID(), UUID.randomUUID(), "1.0", "nonce", Instant.now(), null
        );
        session.addPlayer(1, UUID.randomUUID());
        session.searchPlayers();
        assertThat(session.getState()).isEqualTo(GameSessionState.SEARCHING_PLAYER);

        UUID player2Id = UUID.randomUUID();
        when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
        when(queueService.dequeueNext(machineId.toString())).thenReturn(Optional.of(player2Id));

        MatchNextPlayerCommand command = new MatchNextPlayerCommand(session.getSessionId(), machineId);
        MatchNextPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(MatchNextPlayerResult.Success.class);
        MatchNextPlayerResult.Success success = (MatchNextPlayerResult.Success) result;
        assertThat(success.matchFound()).isTrue();
        assertThat(session.getState()).isEqualTo(GameSessionState.MATCH_FOUND);

        verify(sessionRepository).save(session);
        verify(lobbyNotificationService).notifyMatchFound(session.getSessionId(), session.getMachineId());
    }
}

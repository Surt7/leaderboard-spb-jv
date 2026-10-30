package com.worldpinball.leaderboard.application.features.gamesessions.Public;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore.RecordBallScoreHandlerImpl;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore.RecordBallScoreResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore.commands.RecordBallScoreCommand;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@DisplayName("RecordBallScoreHandler Tests")
class RecordBallScoreHandlerTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private LobbyNotificationService lobbyNotifier;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-30T10:00:00Z"), ZoneOffset.UTC);

    private RecordBallScoreHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new RecordBallScoreHandlerImpl(gameSessionRepository, lobbyNotifier, clock);
    }

    @Test
    @DisplayName("Returns SessionNotFound when session does not exist")
    void handle_whenSessionNotFound_shouldReturnSessionNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(gameSessionRepository.findByIdWithPlayers(sessionId)).thenReturn(Optional.empty());

        RecordBallScoreCommand command = new RecordBallScoreCommand(sessionId, UUID.randomUUID(), 1, 1, 50_000L);
        RecordBallScoreResult result = sut.handle(command);

        assertThat(result).isInstanceOf(RecordBallScoreResult.SessionNotFound.class);
        RecordBallScoreResult.SessionNotFound notFound = (RecordBallScoreResult.SessionNotFound) result;
        assertThat(notFound.sessionId()).isEqualTo(sessionId);
        verify(gameSessionRepository, never()).save(any());
        verifyNoInteractions(lobbyNotifier);
    }

    @Test
    @DisplayName("Returns DeviceMismatch when reporting deviceId does not match session deviceId")
    void handle_whenDeviceIdMismatch_shouldReturnDeviceMismatch() {
        UUID expectedDeviceId = UUID.randomUUID();
        GameSession session = new GameSession(
                UUID.randomUUID(), expectedDeviceId, UUID.randomUUID(), "1.0", "nonce", Instant.now(clock), null
        );
        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        UUID wrongDeviceId = UUID.randomUUID();
        RecordBallScoreCommand command = new RecordBallScoreCommand(session.getSessionId(), wrongDeviceId, 1, 1, 50_000L);
        RecordBallScoreResult result = sut.handle(command);

        assertThat(result).isInstanceOf(RecordBallScoreResult.DeviceMismatch.class);
        RecordBallScoreResult.DeviceMismatch mismatch = (RecordBallScoreResult.DeviceMismatch) result;
        assertThat(mismatch.actualDeviceId()).isEqualTo(wrongDeviceId);
        verify(gameSessionRepository, never()).save(any());
        verifyNoInteractions(lobbyNotifier);
    }

    @Test
    @DisplayName("Records ball score, updates session and notifies lobby on valid command")
    void handle_whenValidCommand_shouldRecordBallScoreUpdateSessionAndNotify() {
        UUID deviceId = UUID.randomUUID();
        GameSession session = new GameSession(
                UUID.randomUUID(), deviceId, UUID.randomUUID(), "1.0", "nonce", Instant.now(clock), null
        );
        session.addPlayer(1, UUID.randomUUID());
        session.launch(Instant.now(clock));

        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        RecordBallScoreCommand command = new RecordBallScoreCommand(session.getSessionId(), deviceId, 1, 2, 850_000L);
        RecordBallScoreResult result = sut.handle(command);

        assertThat(result).isInstanceOf(RecordBallScoreResult.Success.class);
        verify(gameSessionRepository).save(session);
        verify(lobbyNotifier).notifyScoreSubmitted(session.getSessionId(), 1, 850_000L);
    }
}

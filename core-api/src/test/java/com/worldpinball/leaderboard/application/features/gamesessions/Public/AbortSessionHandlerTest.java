package com.worldpinball.leaderboard.application.features.gamesessions.Public;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession.AbortSessionHandlerImpl;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession.AbortSessionResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession.commands.AbortSessionCommand;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.domain.enums.GameSessionState;
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
@DisplayName("AbortSessionHandler Tests")
class AbortSessionHandlerTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private LobbyNotificationService lobbyNotifier;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-29T14:00:00Z"), ZoneOffset.UTC);

    private AbortSessionHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new AbortSessionHandlerImpl(gameSessionRepository, lobbyNotifier, clock);
    }

    @Test
    @DisplayName("Returns SessionNotFound when game session does not exist")
    void handle_whenSessionNotFound_shouldReturnSessionNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(gameSessionRepository.findByIdWithPlayers(sessionId)).thenReturn(Optional.empty());

        AbortSessionCommand command = new AbortSessionCommand(sessionId);
        AbortSessionResult result = sut.handle(command);

        assertThat(result).isInstanceOf(AbortSessionResult.SessionNotFound.class);
        AbortSessionResult.SessionNotFound notFound = (AbortSessionResult.SessionNotFound) result;
        assertThat(notFound.sessionId()).isEqualTo(sessionId);
        verify(gameSessionRepository, never()).save(any());
        verifyNoInteractions(lobbyNotifier);
    }

    @Test
    @DisplayName("Aborts active session, updates state to ABORTED, saves and notifies lobby")
    void handle_whenSessionIsActive_shouldAbortSaveAndNotify() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0", "nonce", Instant.now(clock), null
        );
        session.addPlayer(1, UUID.randomUUID());
        session.launch(Instant.now(clock));

        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        AbortSessionCommand command = new AbortSessionCommand(session.getSessionId());
        AbortSessionResult result = sut.handle(command);

        assertThat(result).isInstanceOf(AbortSessionResult.Success.class);
        AbortSessionResult.Success success = (AbortSessionResult.Success) result;
        assertThat(success.sessionId()).isEqualTo(session.getSessionId());
        assertThat(session.getState()).isEqualTo(GameSessionState.ABORTED);

        verify(gameSessionRepository).save(session);
        verify(lobbyNotifier).notifySessionAborted(session.getSessionId());
    }

    @Test
    @DisplayName("Returns CannotAbortSession when session is already closed (ENDED)")
    void handle_whenSessionAlreadyEnded_shouldReturnCannotAbortSession() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0", "nonce", Instant.now(clock), null
        );
        session.addPlayer(1, UUID.randomUUID());
        session.launch(Instant.now(clock));
        session.endSession(Instant.now(clock).plusSeconds(300));

        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        AbortSessionCommand command = new AbortSessionCommand(session.getSessionId());
        AbortSessionResult result = sut.handle(command);

        assertThat(result).isInstanceOf(AbortSessionResult.CannotAbortSession.class);
        verify(gameSessionRepository, never()).save(any());
        verifyNoInteractions(lobbyNotifier);
    }
}

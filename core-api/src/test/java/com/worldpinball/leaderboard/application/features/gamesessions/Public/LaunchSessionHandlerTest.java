package com.worldpinball.leaderboard.application.features.gamesessions.Public;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession.LaunchSessionHandlerImpl;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession.LaunchSessionResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession.commands.LaunchSessionCommand;
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
@DisplayName("LaunchSessionHandler Tests")
class LaunchSessionHandlerTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private LobbyNotificationService lobbyNotifier;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneOffset.UTC);

    private LaunchSessionHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new LaunchSessionHandlerImpl(gameSessionRepository, lobbyNotifier, clock);
    }

    @Test
    @DisplayName("Returns SessionNotFound when game session does not exist")
    void handle_whenSessionNotFound_shouldReturnSessionNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(gameSessionRepository.findByIdWithPlayers(sessionId)).thenReturn(Optional.empty());

        LaunchSessionCommand command = new LaunchSessionCommand(sessionId);
        LaunchSessionResult result = sut.handle(command);

        assertThat(result).isInstanceOf(LaunchSessionResult.SessionNotFound.class);
        LaunchSessionResult.SessionNotFound notFound = (LaunchSessionResult.SessionNotFound) result;
        assertThat(notFound.sessionId()).isEqualTo(sessionId);
        verify(gameSessionRepository, never()).save(any());
        verifyNoInteractions(lobbyNotifier);
    }

    @Test
    @DisplayName("Launches session, updates state to STARTED, saves and notifies lobby")
    void handle_whenSessionHasPlayers_shouldStartSessionAndSave() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(clock), null
        );
        session.addPlayer(1, UUID.randomUUID());
        assertThat(session.getState()).isEqualTo(GameSessionState.JOINED);

        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        LaunchSessionCommand command = new LaunchSessionCommand(session.getSessionId());
        LaunchSessionResult result = sut.handle(command);

        assertThat(result).isInstanceOf(LaunchSessionResult.Success.class);
        LaunchSessionResult.Success success = (LaunchSessionResult.Success) result;
        assertThat(success.sessionId()).isEqualTo(session.getSessionId());
        assertThat(session.getState()).isEqualTo(GameSessionState.STARTED);

        verify(gameSessionRepository).save(session);
        verify(lobbyNotifier).notifySessionLaunched(session.getSessionId());
    }

    @Test
    @DisplayName("Returns CannotStartSession when session has no players")
    void handle_whenSessionHasNoPlayers_shouldReturnCannotStartSession() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(clock), null
        );
        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        LaunchSessionCommand command = new LaunchSessionCommand(session.getSessionId());
        LaunchSessionResult result = sut.handle(command);

        assertThat(result).isInstanceOf(LaunchSessionResult.CannotStartSession.class);
        LaunchSessionResult.CannotStartSession cannotStart = (LaunchSessionResult.CannotStartSession) result;
        assertThat(cannotStart.message()).containsIgnoringCase("at least one player");

        verify(gameSessionRepository, never()).save(any());
        verifyNoInteractions(lobbyNotifier);
    }

    @Test
    @DisplayName("Returns CannotStartSession when session has already ended")
    void handle_whenSessionAlreadyEnded_shouldReturnCannotStartSession() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(clock), null
        );
        session.addPlayer(1, UUID.randomUUID());
        session.launch(Instant.now(clock));
        session.endSession(Instant.now(clock).plusSeconds(300));
        assertThat(session.getState()).isEqualTo(GameSessionState.ENDED);

        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        LaunchSessionCommand command = new LaunchSessionCommand(session.getSessionId());
        LaunchSessionResult result = sut.handle(command);

        assertThat(result).isInstanceOf(LaunchSessionResult.CannotStartSession.class);
        verify(gameSessionRepository, never()).save(any());
        verifyNoInteractions(lobbyNotifier);
    }
}

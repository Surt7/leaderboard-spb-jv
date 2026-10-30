package com.worldpinball.leaderboard.application.features.gamesessions.Public;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer.KickPlayerHandlerImpl;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer.KickPlayerResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer.commands.KickPlayerCommand;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KickPlayerHandler Tests")
class KickPlayerHandlerTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private LobbyNotificationService lobbyNotifier;

    private KickPlayerHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new KickPlayerHandlerImpl(gameSessionRepository, lobbyNotifier);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5, -1, 10})
    @DisplayName("Returns InvalidSlot when slot number is not between 1 and 4")
    void handle_whenSlotInvalid_shouldReturnInvalidSlot(int invalidSlot) {
        KickPlayerCommand command = new KickPlayerCommand(UUID.randomUUID(), invalidSlot);
        KickPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(KickPlayerResult.InvalidSlot.class);
        KickPlayerResult.InvalidSlot res = (KickPlayerResult.InvalidSlot) result;
        assertThat(res.slotNumber()).isEqualTo(invalidSlot);
        verifyNoInteractions(gameSessionRepository);
    }

    @Test
    @DisplayName("Returns SessionNotFound when session does not exist")
    void handle_whenSessionDoesntExist_shouldReturnSessionNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(gameSessionRepository.findByIdWithPlayers(sessionId)).thenReturn(Optional.empty());

        KickPlayerCommand command = new KickPlayerCommand(sessionId, 1);
        KickPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(KickPlayerResult.SessionNotFound.class);
        KickPlayerResult.SessionNotFound res = (KickPlayerResult.SessionNotFound) result;
        assertThat(res.sessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("Returns CannotRemovePlayerActiveSession when session has already started")
    void handle_whenSessionAlreadyStarted_shouldReturnCannotRemovePlayerActiveSession() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(), null
        );
        session.addPlayer(1, UUID.randomUUID(), "AAA", false);
        session.launch(Instant.now());

        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        KickPlayerCommand command = new KickPlayerCommand(session.getSessionId(), 1);
        KickPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(KickPlayerResult.CannotRemovePlayerActiveSession.class);
        verify(gameSessionRepository, never()).save(any());
        verifyNoInteractions(lobbyNotifier);
    }

    @Test
    @DisplayName("Returns PlayerNotFound when the specified slot is empty")
    void handle_whenPlayerNotInSlot_shouldReturnPlayerNotFound() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(), null
        );
        session.addPlayer(1, UUID.randomUUID(), "AAA", false);

        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        KickPlayerCommand command = new KickPlayerCommand(session.getSessionId(), 2);
        KickPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(KickPlayerResult.PlayerNotFound.class);
        KickPlayerResult.PlayerNotFound res = (KickPlayerResult.PlayerNotFound) result;
        assertThat(res.slotNumber()).isEqualTo(2);
        verify(gameSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Removes player, saves session and notifies lobby when player is in slot and session is joinable")
    void handle_whenPlayerInSlotAndSessionNotStarted_shouldRemovePlayerAndUpdateSession() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(), null
        );
        session.addPlayer(1, UUID.randomUUID(), "AAA", false);
        session.addPlayer(2, UUID.randomUUID(), "BBB", false);

        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        KickPlayerCommand command = new KickPlayerCommand(session.getSessionId(), 2);
        KickPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(KickPlayerResult.Success.class);
        KickPlayerResult.Success res = (KickPlayerResult.Success) result;
        assertThat(res.slotNumber()).isEqualTo(2);
        assertThat(session.getPlayers()).hasSize(1);
        assertThat(session.getPlayers().stream().noneMatch(p -> p.getSlotNumber() == 2)).isTrue();

        verify(gameSessionRepository).save(session);
        verify(lobbyNotifier).notifyLobbyUpdated(session.getSessionId());
    }
}

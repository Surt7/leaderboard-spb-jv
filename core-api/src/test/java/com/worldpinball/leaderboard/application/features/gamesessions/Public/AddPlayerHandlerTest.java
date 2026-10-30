package com.worldpinball.leaderboard.application.features.gamesessions.Public;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.AddPlayerHandlerImpl;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.AddPlayerResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.commands.AddPlayerCommand;
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
@DisplayName("AddPlayerHandler Tests")
class AddPlayerHandlerTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private LobbyNotificationService lobbyNotifier;

    private AddPlayerHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new AddPlayerHandlerImpl(gameSessionRepository, lobbyNotifier);
    }

    @Test
    @DisplayName("Adds player successfully when session exists and slot is free")
    void handle_whenSessionExistsAndSlotAvailable_shouldAddPlayerAndReturnSuccess() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(), null
        );
        UUID userId = UUID.randomUUID();

        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        AddPlayerCommand command = new AddPlayerCommand(session.getSessionId(), 1, userId, "AAA", false);
        AddPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(AddPlayerResult.Success.class);
        AddPlayerResult.Success success = (AddPlayerResult.Success) result;
        assertThat(success.slotNumber()).isEqualTo(1);
        assertThat(success.sessionId()).isEqualTo(session.getSessionId());

        verify(gameSessionRepository).save(session);
        verify(lobbyNotifier).notifyLobbyUpdated(session.getSessionId());
    }

    @Test
    @DisplayName("Returns SessionNotFound when session does not exist")
    void handle_whenSessionDoesntExist_shouldReturnResultSessionNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(gameSessionRepository.findByIdWithPlayers(sessionId)).thenReturn(Optional.empty());

        AddPlayerCommand command = new AddPlayerCommand(sessionId, 1, UUID.randomUUID(), "AAA", false);
        AddPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(AddPlayerResult.SessionNotFound.class);
        verify(gameSessionRepository, never()).save(any());
        verifyNoInteractions(lobbyNotifier);
    }

    @Test
    @DisplayName("Returns SessionNotJoinable when session has already ended")
    void handle_whenSessionIsEnded_shouldReturnResultSessionNotJoinable() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(), null
        );
        session.addPlayer(1, UUID.randomUUID());
        session.launch(Instant.now());
        session.endSession(Instant.now());

        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        AddPlayerCommand command = new AddPlayerCommand(session.getSessionId(), 2, UUID.randomUUID(), "BBB", false);
        AddPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(AddPlayerResult.SessionNotJoinable.class);
        verify(gameSessionRepository, never()).save(any());
        verifyNoInteractions(lobbyNotifier);
    }

    @Test
    @DisplayName("Returns RegisteredUserRequired when mode demands registered user but userId is null")
    void handle_whenTournamentRequiresRegisteredUserAndUserIdIsNull_shouldReturnRegisteredUserRequired() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(), null
        );
        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        AddPlayerCommand command = new AddPlayerCommand(session.getSessionId(), 1, null, "AAA", true);
        AddPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(AddPlayerResult.RegisteredUserRequired.class);
        verify(gameSessionRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5, -1, 9})
    @DisplayName("Returns InvalidSlot when slot number is out of bounds (1..4)")
    void handle_whenSlotIsInvalid_shouldReturnInvalidSlot(int invalidSlot) {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(), null
        );
        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        AddPlayerCommand command = new AddPlayerCommand(session.getSessionId(), invalidSlot, UUID.randomUUID(), "AAA", false);
        AddPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(AddPlayerResult.InvalidSlot.class);
        AddPlayerResult.InvalidSlot res = (AddPlayerResult.InvalidSlot) result;
        assertThat(res.slotNumber()).isEqualTo(invalidSlot);
    }

    @Test
    @DisplayName("Returns UserAlreadyRegistered when user is already in another slot")
    void handle_whenUserIsAlreadyRegistered_shouldReturnUserAlreadyRegistered() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(), null
        );
        UUID userId = UUID.randomUUID();
        session.addPlayer(1, userId);

        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        AddPlayerCommand command = new AddPlayerCommand(session.getSessionId(), 2, userId, "AAA", false);
        AddPlayerResult result = sut.handle(command);

        assertThat(result).isInstanceOf(AddPlayerResult.UserAlreadyRegistered.class);
        AddPlayerResult.UserAlreadyRegistered res = (AddPlayerResult.UserAlreadyRegistered) result;
        assertThat(res.userId()).isEqualTo(userId);
    }
}

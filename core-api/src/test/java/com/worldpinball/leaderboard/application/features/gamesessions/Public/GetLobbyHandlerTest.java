package com.worldpinball.leaderboard.application.features.gamesessions.Public;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.GetLobbyHandlerImpl;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.GetLobbyResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.dtos.LobbySlotDto;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.queries.GetLobbyQuery;
import com.worldpinball.leaderboard.domain.entities.GameSession;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetLobbyHandler Tests")
class GetLobbyHandlerTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    private GetLobbyHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new GetLobbyHandlerImpl(gameSessionRepository);
    }

    @Test
    @DisplayName("Returns SessionNotFound when session does not exist")
    void handle_whenSessionDoesntExist_shouldReturnSessionNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(gameSessionRepository.findByIdWithPlayers(sessionId)).thenReturn(Optional.empty());

        GetLobbyQuery query = new GetLobbyQuery(sessionId);
        GetLobbyResult result = sut.handle(query);

        assertThat(result).isInstanceOf(GetLobbyResult.SessionNotFound.class);
        GetLobbyResult.SessionNotFound notFound = (GetLobbyResult.SessionNotFound) result;
        assertThat(notFound.sessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("Returns four unoccupied slots when session has no registered players")
    void handle_whenSessionHasNoPlayers_shouldReturnSuccessWithFourEmptySlots() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(), null
        );
        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        GetLobbyQuery query = new GetLobbyQuery(session.getSessionId());
        GetLobbyResult result = sut.handle(query);

        assertThat(result).isInstanceOf(GetLobbyResult.Success.class);
        GetLobbyResult.Success success = (GetLobbyResult.Success) result;
        assertThat(success.lobby().sessionId()).isEqualTo(session.getSessionId());
        assertThat(success.lobby().sessionState()).isEqualTo("CREATED");
        assertThat(success.lobby().slots()).hasSize(4);

        for (int i = 1; i <= 4; i++) {
            final int slotNum = i;
            LobbySlotDto slot = success.lobby().slots().stream()
                    .filter(s -> s.slotNumber() == slotNum)
                    .findFirst()
                    .orElseThrow();
            assertThat(slot.isOccupied()).isFalse();
            assertThat(slot.userId()).isNull();
            assertThat(slot.guestInitials()).isNull();
        }
    }

    @Test
    @DisplayName("Correctly maps registered and guest players across the 4 lobby slots")
    void handle_whenSessionHasRegisteredAndGuestPlayers_shouldMapSlotsCorrectly() {
        GameSession session = new GameSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "1.0.0", "nonce", Instant.now(), null
        );
        UUID user1Id = UUID.randomUUID();
        session.addPlayer(1, user1Id, null, true);
        session.addPlayer(2, null, "BOB", false);

        when(gameSessionRepository.findByIdWithPlayers(session.getSessionId())).thenReturn(Optional.of(session));

        GetLobbyQuery query = new GetLobbyQuery(session.getSessionId());
        GetLobbyResult result = sut.handle(query);

        assertThat(result).isInstanceOf(GetLobbyResult.Success.class);
        GetLobbyResult.Success success = (GetLobbyResult.Success) result;

        // Slot 1: Registered User
        LobbySlotDto slot1 = success.lobby().slots().stream().filter(s -> s.slotNumber() == 1).findFirst().orElseThrow();
        assertThat(slot1.isOccupied()).isTrue();
        assertThat(slot1.userId()).isEqualTo(user1Id);
        assertThat(slot1.guestInitials()).isNull();

        // Slot 2: Guest Player
        LobbySlotDto slot2 = success.lobby().slots().stream().filter(s -> s.slotNumber() == 2).findFirst().orElseThrow();
        assertThat(slot2.isOccupied()).isTrue();
        assertThat(slot2.userId()).isNull();
        assertThat(slot2.guestInitials()).isEqualTo("BOB");

        // Slots 3 & 4: Unoccupied
        LobbySlotDto slot3 = success.lobby().slots().stream().filter(s -> s.slotNumber() == 3).findFirst().orElseThrow();
        assertThat(slot3.isOccupied()).isFalse();

        LobbySlotDto slot4 = success.lobby().slots().stream().filter(s -> s.slotNumber() == 4).findFirst().orElseThrow();
        assertThat(slot4.isOccupied()).isFalse();
    }
}

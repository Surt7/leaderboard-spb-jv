package com.worldpinball.leaderboard.application.features.gamesessions.Public;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.SubmitScoreHandlerImpl;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.SubmitScoreResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.commands.SubmitScoreCommand;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.application.services.RealtimeLeaderboardService;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.entities.ScoreEntry;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import com.worldpinball.leaderboard.infrastructure.ScoreEntryRepository;
import com.worldpinball.leaderboard.infrastructure.outbox.DomainEventOutboxRelay;
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
@DisplayName("SubmitScoreHandler Tests")
class SubmitScoreHandlerTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private ScoreEntryRepository scoreEntryRepository;

    @Mock
    private DomainEventOutboxRelay domainEventOutboxRelay;

    @Mock
    private RealtimeLeaderboardService realtimeLeaderboardService;

    @Mock
    private LobbyNotificationService lobbyNotifier;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-30T10:00:00Z"), ZoneOffset.UTC);

    private SubmitScoreHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new SubmitScoreHandlerImpl(
                gameSessionRepository,
                machineRepository,
                scoreEntryRepository,
                domainEventOutboxRelay,
                realtimeLeaderboardService,
                lobbyNotifier,
                clock
        );
    }

    @Test
    @DisplayName("Returns SlotAlreadyOccupied when score for slot already recorded")
    void handle_whenScoreAlreadyExistsForSlot_returnsSlotAlreadyOccupied() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID machineId = UUID.randomUUID();
        UUID modeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        GameSession session = new GameSession(
                machineId, deviceId, modeId, "1.0", "nonce-123", Instant.now(clock), sessionId
        );
        session.addPlayer(1, userId, "USR", false);
        session.launch(Instant.now(clock));
        when(gameSessionRepository.findByIdWithPlayers(sessionId)).thenReturn(Optional.of(session));

        ScoreEntry existing = new ScoreEntry(
                sessionId, 1, deviceId, machineId, UUID.randomUUID(), modeId, 500_000L, Instant.now(clock), userId
        );
        when(scoreEntryRepository.findBySessionIdAndSlotNumber(sessionId, 1)).thenReturn(Optional.of(existing));

        SubmitScoreCommand command = new SubmitScoreCommand(
                sessionId, 1, deviceId, machineId, UUID.randomUUID(), modeId, 750_000L, Instant.now(clock), userId
        );
        SubmitScoreResult result = sut.handle(command);

        assertThat(result).isInstanceOf(SubmitScoreResult.SlotAlreadyOccupied.class);
        SubmitScoreResult.SlotAlreadyOccupied occupied = (SubmitScoreResult.SlotAlreadyOccupied) result;
        assertThat(occupied.slotNumber()).isEqualTo(1);

        verify(scoreEntryRepository, never()).save(any());
        verifyNoInteractions(domainEventOutboxRelay);
        verifyNoInteractions(realtimeLeaderboardService);
    }

    @Test
    @DisplayName("Persists score, relays outbox event, updates Redis leaderboard and notifies lobby on valid score")
    void handle_whenValidScore_persistsScoreRelaysOutboxAndUpdatesLeaderboard() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID machineId = UUID.randomUUID();
        UUID ipId = UUID.randomUUID();
        UUID modeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        GameSession session = new GameSession(
                machineId, deviceId, modeId, "1.0", "nonce-123", Instant.now(clock), sessionId
        );
        session.addPlayer(1, userId, "USR", false);
        session.launch(Instant.now(clock));
        when(gameSessionRepository.findByIdWithPlayers(sessionId)).thenReturn(Optional.of(session));

        when(scoreEntryRepository.findBySessionIdAndSlotNumber(sessionId, 1)).thenReturn(Optional.empty());
        when(scoreEntryRepository.save(any(ScoreEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitScoreCommand command = new SubmitScoreCommand(
                sessionId, 1, deviceId, machineId, ipId, modeId, 1_250_000L, null, userId
        );
        SubmitScoreResult result = sut.handle(command);

        assertThat(result).isInstanceOf(SubmitScoreResult.Success.class);
        SubmitScoreResult.Success success = (SubmitScoreResult.Success) result;
        assertThat(success.scoreEntryIds()).hasSize(1);

        verify(scoreEntryRepository).save(any(ScoreEntry.class));
        verify(domainEventOutboxRelay).persistOutboxMessagesAndNotifyAfterCommit(any(ScoreEntry.class));
        verify(realtimeLeaderboardService).addScore(any(UUID.class), eq(modeId), any(UUID.class), eq(1_250_000L));
        verify(realtimeLeaderboardService).invalidateCachedLeaderboard(any(UUID.class), eq(modeId));
        verify(lobbyNotifier).notifyScoreSubmitted(eq(sessionId), eq(1), eq(1_250_000L));
    }
}

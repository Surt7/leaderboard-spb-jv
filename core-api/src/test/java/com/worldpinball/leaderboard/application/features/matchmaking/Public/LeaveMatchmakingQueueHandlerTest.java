package com.worldpinball.leaderboard.application.features.matchmaking.Public;

import com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue.LeaveMatchmakingQueueHandlerImpl;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue.LeaveMatchmakingQueueResult;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue.LeaveMatchmakingQueueCommand;
import com.worldpinball.leaderboard.application.services.MatchmakingQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveMatchmakingQueueHandler Tests")
class LeaveMatchmakingQueueHandlerTest {

    @Mock
    private MatchmakingQueueService queueService;

    private LeaveMatchmakingQueueHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new LeaveMatchmakingQueueHandlerImpl(queueService);
    }

    @Test
    @DisplayName("Removes player and returns Success when player is in matchmaking queue")
    void handle_whenPlayerInQueue_shouldRemoveAndReturnSuccess() {
        UUID machineId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(queueService.remove(machineId.toString(), playerId)).thenReturn(true);

        LeaveMatchmakingQueueCommand command = new LeaveMatchmakingQueueCommand(machineId, playerId);
        LeaveMatchmakingQueueResult result = sut.handle(command);

        assertThat(result).isInstanceOf(LeaveMatchmakingQueueResult.Success.class);
        verify(queueService).remove(machineId.toString(), playerId);
    }

    @Test
    @DisplayName("Returns PlayerNotInQueue when player is not found in matchmaking queue")
    void handle_whenPlayerNotInQueue_shouldReturnPlayerNotInQueue() {
        UUID machineId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(queueService.remove(machineId.toString(), playerId)).thenReturn(false);

        LeaveMatchmakingQueueCommand command = new LeaveMatchmakingQueueCommand(machineId, playerId);
        LeaveMatchmakingQueueResult result = sut.handle(command);

        assertThat(result).isInstanceOf(LeaveMatchmakingQueueResult.PlayerNotInQueue.class);
    }
}

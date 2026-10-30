package com.worldpinball.leaderboard.application.features.matchmaking.Public;

import com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue.JoinMatchmakingQueueHandlerImpl;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue.JoinMatchmakingQueueResult;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue.JoinMatchmakingQueueCommand;
import com.worldpinball.leaderboard.application.services.MatchmakingQueueService;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JoinMatchmakingQueueHandler Tests")
class JoinMatchmakingQueueHandlerTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private MatchmakingQueueService queueService;

    private JoinMatchmakingQueueHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new JoinMatchmakingQueueHandlerImpl(machineRepository, queueService);
    }

    @Test
    @DisplayName("Returns MachineNotFound when machine does not exist in repository")
    void handle_whenMachineDoesNotExist_shouldReturnMachineNotFound() {
        UUID machineId = UUID.randomUUID();
        when(machineRepository.existsById(machineId)).thenReturn(false);

        JoinMatchmakingQueueCommand command = new JoinMatchmakingQueueCommand(machineId, UUID.randomUUID());
        JoinMatchmakingQueueResult result = sut.handle(command);

        assertThat(result).isInstanceOf(JoinMatchmakingQueueResult.MachineNotFound.class);
        JoinMatchmakingQueueResult.MachineNotFound res = (JoinMatchmakingQueueResult.MachineNotFound) result;
        assertThat(res.machineId()).isEqualTo(machineId);

        verify(queueService, never()).enqueue(any(), any());
    }

    @Test
    @DisplayName("Enqueues player and returns Success when machine exists")
    void handle_whenMachineExists_shouldReturnSuccess() {
        UUID machineId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(machineRepository.existsById(machineId)).thenReturn(true);

        JoinMatchmakingQueueCommand command = new JoinMatchmakingQueueCommand(machineId, playerId);
        JoinMatchmakingQueueResult result = sut.handle(command);

        assertThat(result).isInstanceOf(JoinMatchmakingQueueResult.Success.class);
        verify(queueService).enqueue(machineId.toString(), playerId);
    }
}

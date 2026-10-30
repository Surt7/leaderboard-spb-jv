package com.worldpinball.leaderboard.api.controllers.Public;

import com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue.JoinMatchmakingQueueCommand;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue.JoinMatchmakingQueueHandler;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue.JoinMatchmakingQueueRequest;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue.JoinMatchmakingQueueResult;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue.LeaveMatchmakingQueueCommand;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue.LeaveMatchmakingQueueHandler;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue.LeaveMatchmakingQueueRequest;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue.LeaveMatchmakingQueueResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchmakingController Tests")
class MatchmakingControllerTest {

    @Mock
    private JoinMatchmakingQueueHandler joinHandler;

    @Mock
    private LeaveMatchmakingQueueHandler leaveHandler;

    private MatchmakingController sut;

    @BeforeEach
    void setUp() {
        sut = new MatchmakingController(joinHandler, leaveHandler);
    }

    @Test
    @DisplayName("Returns 200 OK when join handler succeeds")
    void join_whenHandlerReturnsSuccess_shouldReturnOk() {
        when(joinHandler.handle(any(JoinMatchmakingQueueCommand.class)))
                .thenReturn(new JoinMatchmakingQueueResult.Success());

        JoinMatchmakingQueueRequest request = new JoinMatchmakingQueueRequest(UUID.randomUUID(), UUID.randomUUID());
        ResponseEntity<?> response = sut.join(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Returns 404 NOT_FOUND when machine is not found on join")
    void join_whenHandlerReturnsMachineNotFound_shouldReturnNotFound() {
        UUID machineId = UUID.randomUUID();
        when(joinHandler.handle(any(JoinMatchmakingQueueCommand.class)))
                .thenReturn(new JoinMatchmakingQueueResult.MachineNotFound(machineId));

        JoinMatchmakingQueueRequest request = new JoinMatchmakingQueueRequest(machineId, UUID.randomUUID());
        ResponseEntity<?> response = sut.join(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Returns 200 OK when leave handler succeeds")
    void leave_whenHandlerReturnsSuccess_shouldReturnOk() {
        when(leaveHandler.handle(any(LeaveMatchmakingQueueCommand.class)))
                .thenReturn(new LeaveMatchmakingQueueResult.Success());

        LeaveMatchmakingQueueRequest request = new LeaveMatchmakingQueueRequest(UUID.randomUUID(), UUID.randomUUID());
        ResponseEntity<?> response = sut.leave(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Returns 404 NOT_FOUND when player is not in queue on leave")
    void leave_whenHandlerReturnsPlayerNotInQueue_shouldReturnNotFound() {
        when(leaveHandler.handle(any(LeaveMatchmakingQueueCommand.class)))
                .thenReturn(new LeaveMatchmakingQueueResult.PlayerNotInQueue());

        LeaveMatchmakingQueueRequest request = new LeaveMatchmakingQueueRequest(UUID.randomUUID(), UUID.randomUUID());
        ResponseEntity<?> response = sut.leave(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

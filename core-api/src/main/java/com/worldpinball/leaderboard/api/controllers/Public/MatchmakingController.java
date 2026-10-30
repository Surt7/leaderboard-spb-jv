package com.worldpinball.leaderboard.api.controllers.Public;

import com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue.JoinMatchmakingQueueCommand;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue.JoinMatchmakingQueueHandler;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue.JoinMatchmakingQueueRequest;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue.JoinMatchmakingQueueResult;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue.LeaveMatchmakingQueueCommand;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue.LeaveMatchmakingQueueHandler;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue.LeaveMatchmakingQueueRequest;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue.LeaveMatchmakingQueueResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/matchmaking")
@Tag(name = "Matchmaking", description = "Matchmaking queue operations for pinball machines")
public class MatchmakingController {

    private final JoinMatchmakingQueueHandler joinHandler;
    private final LeaveMatchmakingQueueHandler leaveHandler;

    public MatchmakingController(
            JoinMatchmakingQueueHandler joinHandler,
            LeaveMatchmakingQueueHandler leaveHandler
    ) {
        this.joinHandler = joinHandler;
        this.leaveHandler = leaveHandler;
    }

    @PostMapping("/join")
    @Operation(summary = "Adds a player to the matchmaking queue for a machine")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Player enqueued successfully"),
            @ApiResponse(responseCode = "404", description = "Machine not found")
    })
    public ResponseEntity<?> join(@Valid @RequestBody JoinMatchmakingQueueRequest request) {
        JoinMatchmakingQueueCommand command = new JoinMatchmakingQueueCommand(request.machineId(), request.playerId());
        JoinMatchmakingQueueResult result = joinHandler.handle(command);

        return switch (result) {
            case JoinMatchmakingQueueResult.Success ignored -> ResponseEntity.ok(
                    Map.of("message", "Player enqueued successfully.")
            );
            case JoinMatchmakingQueueResult.MachineNotFound notFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Machine '" + notFound.machineId() + "' was not found."));
        };
    }

    @PostMapping("/leave")
    @Operation(summary = "Removes a player from the matchmaking queue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Player removed from queue"),
            @ApiResponse(responseCode = "404", description = "Player not in queue")
    })
    public ResponseEntity<?> leave(@Valid @RequestBody LeaveMatchmakingQueueRequest request) {
        LeaveMatchmakingQueueCommand command = new LeaveMatchmakingQueueCommand(request.machineId(), request.playerId());
        LeaveMatchmakingQueueResult result = leaveHandler.handle(command);

        return switch (result) {
            case LeaveMatchmakingQueueResult.Success ignored -> ResponseEntity.ok(
                    Map.of("message", "Player removed from queue.")
            );
            case LeaveMatchmakingQueueResult.PlayerNotInQueue ignored -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Player was not found in matchmaking queue."));
        };
    }
}

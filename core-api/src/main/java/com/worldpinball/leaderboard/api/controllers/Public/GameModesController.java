package com.worldpinball.leaderboard.api.controllers.Public;

import com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes.GameModePublicDto;
import com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes.GetAllGameModesHandler;
import com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes.GetAllGameModesQuery;
import com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes.GetAllGameModesResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.Map;

@RestController
@RequestMapping("/api/v1/game-modes")
@Tag(name = "GameModes", description = "Available game modes")
public class GameModesController {

    private final GetAllGameModesHandler handler;

    public GameModesController(GetAllGameModesHandler handler) {
        this.handler = handler;
    }

    @GetMapping
    @Operation(summary = "Returns all available game modes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of active game modes",
                    content = @Content(schema = @Schema(implementation = GameModePublicDto.class))),
            @ApiResponse(responseCode = "404", description = "No game modes found")
    })
    public ResponseEntity<?> getAll() {
        GetAllGameModesResult result = handler.handle(new GetAllGameModesQuery());

        return switch (result) {
            case GetAllGameModesResult.Success s -> ResponseEntity.ok(s.gameModes());
            case GetAllGameModesResult.NotFound ignored -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No game modes found."));
        };
    }
}

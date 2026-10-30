package com.worldpinball.leaderboard.api.controllers.Public;

import com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes.GameModePublicDto;
import com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes.GetAllGameModesHandler;
import com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes.GetAllGameModesQuery;
import com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes.GetAllGameModesResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameModesController Tests")
class GameModesControllerTest {

    @Mock
    private GetAllGameModesHandler handler;

    private GameModesController sut;

    @BeforeEach
    void setUp() {
        sut = new GameModesController(handler);
    }

    @Test
    @DisplayName("Returns 200 OK with game modes when game modes exist")
    void getAll_whenGameModesExist_shouldReturnOk() {
        List<GameModePublicDto> gameModes = List.of(
                new GameModePublicDto(UUID.randomUUID(), "CLASSIC", "Classic", "Standard gameplay")
        );
        when(handler.handle(any(GetAllGameModesQuery.class)))
                .thenReturn(new GetAllGameModesResult.Success(gameModes));

        ResponseEntity<?> response = sut.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(gameModes);
    }

    @Test
    @DisplayName("Returns 404 NOT_FOUND when no game modes exist")
    void getAll_whenNoGameModesExist_shouldReturnNotFound() {
        when(handler.handle(any(GetAllGameModesQuery.class)))
                .thenReturn(new GetAllGameModesResult.NotFound());

        ResponseEntity<?> response = sut.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

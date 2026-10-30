package com.worldpinball.leaderboard.application.features.gamemodes.Public;

import com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes.GetAllGameModesHandlerImpl;
import com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes.GetAllGameModesQuery;
import com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes.GetAllGameModesResult;
import com.worldpinball.leaderboard.domain.entities.GameMode;
import com.worldpinball.leaderboard.infrastructure.GameModeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAllGameModesHandler Tests")
class GetAllGameModesHandlerTest {

    @Mock
    private GameModeRepository gameModeRepository;

    private GetAllGameModesHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new GetAllGameModesHandlerImpl(gameModeRepository);
    }

    @Test
    @DisplayName("Returns Success with list of game modes when active game modes exist")
    void handle_whenGameModesExist_shouldReturnSuccess() {
        GameMode mode1 = new GameMode("CLASSIC", "Classic", "Standard gameplay", true, UUID.randomUUID());
        GameMode mode2 = new GameMode("TIMED", "Timed", "Beat the clock", true, UUID.randomUUID());

        when(gameModeRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(mode1, mode2));

        GetAllGameModesResult result = sut.handle(new GetAllGameModesQuery());

        assertThat(result).isInstanceOf(GetAllGameModesResult.Success.class);
        GetAllGameModesResult.Success success = (GetAllGameModesResult.Success) result;
        assertThat(success.gameModes()).hasSize(2);
        assertThat(success.gameModes().get(0).name()).isEqualTo("Classic");
        assertThat(success.gameModes().get(1).name()).isEqualTo("Timed");
    }

    @Test
    @DisplayName("Returns NotFound when repository contains no active game modes")
    void handle_whenNoGameModesExist_shouldReturnNotFound() {
        when(gameModeRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of());

        GetAllGameModesResult result = sut.handle(new GetAllGameModesQuery());

        assertThat(result).isInstanceOf(GetAllGameModesResult.NotFound.class);
    }
}

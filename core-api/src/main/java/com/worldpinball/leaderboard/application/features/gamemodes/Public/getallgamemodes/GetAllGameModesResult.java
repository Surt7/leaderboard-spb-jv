package com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes;

import java.util.List;

public sealed interface GetAllGameModesResult {
    record Success(List<GameModePublicDto> gameModes) implements GetAllGameModesResult {}
    record NotFound() implements GetAllGameModesResult {}
}

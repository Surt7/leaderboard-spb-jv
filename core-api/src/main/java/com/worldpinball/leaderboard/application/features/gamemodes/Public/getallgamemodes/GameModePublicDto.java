package com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes;

import java.util.UUID;

public record GameModePublicDto(
        UUID modeId,
        String code,
        String name,
        String description
) {}

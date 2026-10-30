package com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession.commands;

import java.util.UUID;

public record CreateGameSessionCommand(
    UUID deviceId,
    String modeCode,
    String clientVersion
) {}

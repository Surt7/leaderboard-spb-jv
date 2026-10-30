package com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.dtos;

import java.util.List;
import java.util.UUID;

public record SessionLobbyDto(
    UUID sessionId,
    String sessionState,
    List<LobbySlotDto> slots
) {}

package com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.dtos;

import com.worldpinball.leaderboard.domain.enums.GameSlotStatus;

import java.util.UUID;

public record LobbySlotDto(
        int slotNumber,
        boolean isOccupied,
        GameSlotStatus status,
        UUID userId,
        String guestInitials
) {
    public LobbySlotDto(int slotNumber, boolean isOccupied, UUID userId, String guestInitials) {
        this(
                slotNumber,
                isOccupied,
                isOccupied ? GameSlotStatus.Occupied : GameSlotStatus.Open,
                userId,
                guestInitials
        );
    }
}

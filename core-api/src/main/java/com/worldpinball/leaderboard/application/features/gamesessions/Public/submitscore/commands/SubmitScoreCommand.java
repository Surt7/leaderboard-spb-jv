package com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.commands;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.SlotScoreDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubmitScoreCommand(
        UUID sessionId,
        UUID deviceId,
        List<SlotScoreDto> scores
) {
    public SubmitScoreCommand(
            UUID sessionId,
            int slotNumber,
            UUID deviceId,
            UUID machineId,
            UUID ipId,
            UUID modeId,
            long score,
            Instant achievedAtUtc,
            UUID userId
    ) {
        this(sessionId, deviceId, List.of(new SlotScoreDto(slotNumber, score)));
    }
}

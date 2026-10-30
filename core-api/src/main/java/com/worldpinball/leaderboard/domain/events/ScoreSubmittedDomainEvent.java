package com.worldpinball.leaderboard.domain.events;

import java.time.Instant;
import java.util.UUID;

public record ScoreSubmittedDomainEvent(
        UUID scoreEntryId,
        UUID sessionId,
        UUID machineId,
        UUID ipId,
        UUID modeId,
        int slotNumber,
        String userId,
        long score,
        Instant achievedAtUtc
) implements DomainEvent {
}

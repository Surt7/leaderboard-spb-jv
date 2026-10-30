package com.worldpinball.leaderboard.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryDto {
    private long rank;
    private UUID scoreEntryId;
    private long score;
    private Instant achievedAtUtc;
    private int slotNumber;
    private UUID userId;
    private String displayName;
    private String initials;
}

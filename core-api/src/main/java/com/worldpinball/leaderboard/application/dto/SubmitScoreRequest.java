package com.worldpinball.leaderboard.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class SubmitScoreRequest {

    @NotNull(message = "SessionId is required")
    private UUID sessionId;

    @Min(value = 1, message = "Slot number must be between 1 and 4")
    @Max(value = 4, message = "Slot number must be between 1 and 4")
    private int slotNumber;

    @NotNull(message = "DeviceId is required")
    private UUID deviceId;

    @NotNull(message = "MachineId is required")
    private UUID machineId;

    @NotNull(message = "IpId is required")
    private UUID ipId;

    @NotNull(message = "ModeId is required")
    private UUID modeId;

    @PositiveOrZero(message = "Score cannot be negative")
    private long score;

    private Instant achievedAtUtc;

    private UUID userId;
}

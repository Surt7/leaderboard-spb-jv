package com.worldpinball.leaderboard.application.features.machines.Admin.getallmachines;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;

import java.time.Instant;
import java.util.UUID;

public record MachineRowDto(
        UUID machineId,
        String machineName,
        String displayName,
        MachineStatus machineStatus,
        String machineNotes,
        @JsonProperty("registredAtUtc")
        Instant registeredAtUtc,
        Instant lastUpdated,
        String serialNumber,
        Instant certExpiresAtUtc,
        Integer certStatus
) {
    public MachineRowDto(
            UUID machineId,
            String machineName,
            String displayName,
            MachineStatus machineStatus,
            String machineNotes,
            Instant registeredAtUtc,
            Instant lastUpdated,
            String serialNumber
    ) {
        this(machineId, machineName, displayName, machineStatus, machineNotes, registeredAtUtc, lastUpdated, serialNumber, null, null);
    }
}

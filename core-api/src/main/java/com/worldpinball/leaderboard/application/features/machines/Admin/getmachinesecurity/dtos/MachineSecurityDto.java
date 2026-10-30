package com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity.dtos;

import java.time.Instant;
import java.util.UUID;

public record MachineSecurityDto(
        UUID machineId,
        String serialNumber,
        String displayName,
        int machineStatus,
        boolean hasAttachedDevice,
        UUID deviceId,
        String devicePhysicalSerial,
        boolean hasCertificate,
        String certSerialNumber,
        String certThumbprintSha256,
        String subjectDn,
        String issuerDn,
        Instant notBeforeUtc,
        Instant notAfterUtc,
        Integer certStatus,
        boolean isExpired,
        boolean isValid,
        String statusExplanation
) {}

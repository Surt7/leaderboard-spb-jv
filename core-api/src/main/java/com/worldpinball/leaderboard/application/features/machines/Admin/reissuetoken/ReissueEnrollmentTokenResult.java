package com.worldpinball.leaderboard.application.features.machines.Admin.reissuetoken;

import com.worldpinball.leaderboard.domain.enums.MachineStatus;

import java.time.Instant;
import java.util.UUID;

public sealed interface ReissueEnrollmentTokenResult {
    record Success(UUID machineId, String enrollmentToken, Instant expiresAtUtc) implements ReissueEnrollmentTokenResult {}
    record MachineNotFound(UUID machineId) implements ReissueEnrollmentTokenResult {}
    record InvalidMachineStatus(UUID machineId, MachineStatus status) implements ReissueEnrollmentTokenResult {}
}

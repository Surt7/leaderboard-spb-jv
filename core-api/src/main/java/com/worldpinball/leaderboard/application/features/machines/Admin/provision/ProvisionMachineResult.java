package com.worldpinball.leaderboard.application.features.machines.Admin.provision;

import java.util.UUID;

public sealed interface ProvisionMachineResult {
    record Success(UUID machineId, String enrollmentToken) implements ProvisionMachineResult {}
    record UnknownIp(String ipSlug) implements ProvisionMachineResult {}
    record SameIdempotencyKey(UUID idempotencyKey) implements ProvisionMachineResult {}
    record DuplicateSerial(String serialNumber) implements ProvisionMachineResult {}
}

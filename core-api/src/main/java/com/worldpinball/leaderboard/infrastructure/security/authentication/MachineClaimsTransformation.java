package com.worldpinball.leaderboard.infrastructure.security.authentication;

import com.worldpinball.leaderboard.application.services.MachineStatusCacheService;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Enriches an authenticated Machine principal with its {@code machine_status} claim,
 * looking up Redis L2 cache first and falling back to PostgreSQL with a 5-minute cache warm-up.
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.Authentification.MachineClaimsTransformation}.
 */
@Component
public class MachineClaimsTransformation {

    public static final String CLAIM_MACHINE_STATUS = "machine_status";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final MachineStatusCacheService cache;
    private final MachineRepository machineRepository;

    public MachineClaimsTransformation(
            MachineStatusCacheService cache,
            MachineRepository machineRepository) {
        this.cache = cache;
        this.machineRepository = machineRepository;
    }

    public ClaimsPrincipal transform(ClaimsPrincipal principal) {
        if (principal == null) {
            return null;
        }

        if (principal.hasClaim(c -> CLAIM_MACHINE_STATUS.equalsIgnoreCase(c.type()))) {
            return principal;
        }

        Optional<String> subOpt = principal.findFirst(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER);
        if (subOpt.isEmpty()) {
            return principal;
        }

        UUID machineId;
        try {
            machineId = UUID.fromString(subOpt.get());
        } catch (IllegalArgumentException e) {
            return principal;
        }

        Optional<String> cachedStatus = cache.getStatus(machineId);
        if (cachedStatus.isPresent()) {
            principal.addClaim(CLAIM_MACHINE_STATUS, cachedStatus.get());
            return principal;
        }

        Optional<Machine> machineOpt = machineRepository.findById(machineId);
        if (machineOpt.isEmpty()) {
            return principal;
        }

        String status = formatStatus(machineOpt.get().getStatus());
        cache.setStatus(machineId, status, CACHE_TTL);
        principal.addClaim(CLAIM_MACHINE_STATUS, status);

        return principal;
    }

    private static String formatStatus(MachineStatus status) {
        if (status == null) {
            return "Unknown";
        }
        return switch (status) {
            case ACTIVE -> "Active";
            case REVOKED -> "Revoked";
            case INACTIVE -> "Inactive";
            case MAINTENANCE -> "Maintenance";
            case PENDING_ENROLLMENT -> "PendingEnrollment";
        };
    }
}

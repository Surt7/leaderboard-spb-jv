package com.worldpinball.leaderboard.infrastructure.security.authentication;

import com.worldpinball.leaderboard.application.services.MachineStatusCacheService;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MachineClaimsTransformation Tests")
class MachineClaimsTransformationTest {

    @Mock
    private MachineStatusCacheService machineStatusCache;

    @Mock
    private MachineRepository machineRepository;

    @Test
    @DisplayName("Uses existing claim if machine_status already present")
    void transform_whenClaimAlreadyPresent_doesNotQueryCacheOrDb() {
        MachineClaimsTransformation sut = new MachineClaimsTransformation(machineStatusCache, machineRepository);

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "Machine",
                List.of(
                        new ClaimsPrincipal.Claim(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER, UUID.randomUUID().toString()),
                        new ClaimsPrincipal.Claim("machine_status", "ACTIVE")
                ),
                List.of("Machine")
        );

        ClaimsPrincipal transformed = sut.transform(principal);

        assertThat(transformed.findFirst("machine_status")).contains("ACTIVE");
        verifyNoInteractions(machineStatusCache);
        verifyNoInteractions(machineRepository);
    }

    @Test
    @DisplayName("Enriches machine_status claim from Redis cache when present")
    void transform_whenStatusInRedisCache_enrichesWithoutDb() {
        UUID machineId = UUID.randomUUID();
        when(machineStatusCache.getStatus(machineId)).thenReturn(Optional.of("Active"));

        MachineClaimsTransformation sut = new MachineClaimsTransformation(machineStatusCache, machineRepository);

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "Machine",
                List.of(new ClaimsPrincipal.Claim(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER, machineId.toString())),
                List.of("Machine")
        );

        ClaimsPrincipal transformed = sut.transform(principal);

        assertThat(transformed.findFirst("machine_status")).contains("Active");
        verify(machineStatusCache).getStatus(machineId);
        verifyNoInteractions(machineRepository);
    }

    @Test
    @DisplayName("Falls back to MachineRepository on cache miss and warms up Redis for 5 minutes")
    void transform_whenCacheMiss_queriesDbAndWarmsCache() {
        UUID machineId = UUID.randomUUID();
        when(machineStatusCache.getStatus(machineId)).thenReturn(Optional.empty());

        Machine machine = new Machine(
                UUID.randomUUID(),
                "WP-SN-1234",
                "Godzilla Test",
                Instant.now(),
                UUID.randomUUID(),
                "nonce-test",
                null
        );
        machine.completeEnrollment(Instant.now());
        when(machineRepository.findById(machineId)).thenReturn(Optional.of(machine));

        MachineClaimsTransformation sut = new MachineClaimsTransformation(machineStatusCache, machineRepository);

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "Machine",
                List.of(new ClaimsPrincipal.Claim(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER, machineId.toString())),
                List.of("Machine")
        );

        ClaimsPrincipal transformed = sut.transform(principal);

        assertThat(transformed.findFirst("machine_status")).contains("Active");
        verify(machineRepository).findById(machineId);
        verify(machineStatusCache).setStatus(eq(machineId), eq("Active"), eq(Duration.ofMinutes(5)));
    }
}

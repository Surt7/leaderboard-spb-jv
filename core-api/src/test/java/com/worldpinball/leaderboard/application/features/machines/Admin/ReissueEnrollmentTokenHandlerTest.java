package com.worldpinball.leaderboard.application.features.machines.Admin;

import com.worldpinball.leaderboard.application.features.machines.Admin.reissuetoken.ReissueEnrollmentTokenHandlerImpl;
import com.worldpinball.leaderboard.application.features.machines.Admin.reissuetoken.ReissueEnrollmentTokenResult;
import com.worldpinball.leaderboard.application.features.machines.Admin.reissuetoken.commands.ReissueEnrollmentTokenCommand;
import com.worldpinball.leaderboard.application.security.secrets.SecretGenerationResult;
import com.worldpinball.leaderboard.application.security.secrets.SecretService;
import com.worldpinball.leaderboard.domain.entities.EnrollmentToken;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import com.worldpinball.leaderboard.infrastructure.EnrollmentTokenRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReissueEnrollmentTokenHandler Tests")
class ReissueEnrollmentTokenHandlerTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private EnrollmentTokenRepository enrollmentTokenRepository;

    @Mock
    private SecretService secretService;

    private ReissueEnrollmentTokenHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ReissueEnrollmentTokenHandlerImpl(machineRepository, enrollmentTokenRepository, secretService);
    }

    @Test
    @DisplayName("Returns MachineNotFound when machine does not exist")
    void handle_whenMachineDoesNotExist_shouldReturnMachineNotFound() {
        UUID machineId = UUID.randomUUID();
        when(machineRepository.findById(machineId)).thenReturn(Optional.empty());

        ReissueEnrollmentTokenCommand command = new ReissueEnrollmentTokenCommand(machineId, "admin-123");
        ReissueEnrollmentTokenResult result = sut.handle(command);

        assertThat(result).isInstanceOf(ReissueEnrollmentTokenResult.MachineNotFound.class);
        ReissueEnrollmentTokenResult.MachineNotFound notFound = (ReissueEnrollmentTokenResult.MachineNotFound) result;
        assertThat(notFound.machineId()).isEqualTo(machineId);

        verify(enrollmentTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Returns InvalidMachineStatus when machine is not in PendingEnrollment status")
    void handle_whenMachineStatusIsNotPendingEnrollment_shouldReturnInvalidMachineStatus() {
        Machine machine = new Machine(
                UUID.randomUUID(),
                "WP-TEST-ACTIVE",
                "Test Active Pinball",
                Instant.now(),
                UUID.randomUUID(),
                "nonce-123",
                null
        );
        machine.completeEnrollment(Instant.now());
        assertThat(machine.getStatus()).isEqualTo(MachineStatus.ACTIVE);

        when(machineRepository.findById(machine.getMachineId())).thenReturn(Optional.of(machine));

        ReissueEnrollmentTokenCommand command = new ReissueEnrollmentTokenCommand(machine.getMachineId(), "admin-123");
        ReissueEnrollmentTokenResult result = sut.handle(command);

        assertThat(result).isInstanceOf(ReissueEnrollmentTokenResult.InvalidMachineStatus.class);
        ReissueEnrollmentTokenResult.InvalidMachineStatus invalidStatus = (ReissueEnrollmentTokenResult.InvalidMachineStatus) result;
        assertThat(invalidStatus.machineId()).isEqualTo(machine.getMachineId());
        assertThat(invalidStatus.status()).isEqualTo(MachineStatus.ACTIVE);

        verify(enrollmentTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reissues token, persists it and returns Success when machine is in PendingEnrollment status")
    void handle_whenMachineIsPendingEnrollment_shouldSaveTokenAndReturnSuccess() {
        Machine machine = new Machine(
                UUID.randomUUID(),
                "WP-TEST-PENDING",
                "Test Pending Pinball",
                Instant.now(),
                UUID.randomUUID(),
                "nonce-456",
                null
        );
        assertThat(machine.getStatus()).isEqualTo(MachineStatus.PENDING_ENROLLMENT);

        when(machineRepository.findById(machine.getMachineId())).thenReturn(Optional.of(machine));

        String plainToken = "reissued-plain-token-base64";
        String tokenHash = "token-hash-hex";
        when(secretService.generate()).thenReturn(new SecretGenerationResult(plainToken, tokenHash, "encrypted-secret"));

        ReissueEnrollmentTokenCommand command = new ReissueEnrollmentTokenCommand(machine.getMachineId(), "admin-123");
        ReissueEnrollmentTokenResult result = sut.handle(command);

        assertThat(result).isInstanceOf(ReissueEnrollmentTokenResult.Success.class);
        ReissueEnrollmentTokenResult.Success success = (ReissueEnrollmentTokenResult.Success) result;
        assertThat(success.machineId()).isEqualTo(machine.getMachineId());
        assertThat(success.enrollmentToken()).isEqualTo(plainToken);
        assertThat(success.expiresAtUtc()).isNotNull();

        verify(enrollmentTokenRepository).save(any(EnrollmentToken.class));
    }
}

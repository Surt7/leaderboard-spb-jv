package com.worldpinball.leaderboard.application.features.machines.Admin;

import com.worldpinball.leaderboard.application.features.machines.Admin.provision.ProvisionMachineHandlerImpl;
import com.worldpinball.leaderboard.application.features.machines.Admin.provision.ProvisionMachineResult;
import com.worldpinball.leaderboard.application.features.machines.Admin.provision.commands.ProvisionMachineCommand;
import com.worldpinball.leaderboard.application.security.secrets.SecretGenerationResult;
import com.worldpinball.leaderboard.application.security.secrets.SecretService;
import com.worldpinball.leaderboard.domain.entities.EnrollmentToken;
import com.worldpinball.leaderboard.domain.entities.IpLicense;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.infrastructure.EnrollmentTokenRepository;
import com.worldpinball.leaderboard.infrastructure.IpLicenseRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MachineProvisioningHandler Tests")
class MachineProvisioningHandlerTest {

    @Mock
    private IpLicenseRepository ipLicenseRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private EnrollmentTokenRepository enrollmentTokenRepository;

    @Mock
    private SecretService secretService;

    private ProvisionMachineHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ProvisionMachineHandlerImpl(
                ipLicenseRepository,
                machineRepository,
                enrollmentTokenRepository,
                secretService
        );
    }

    @Test
    @DisplayName("Returns UnknownIp when IP slug is not found or inactive")
    void handle_whenIpIsUnknown_shouldReturnUnknownIp() {
        String ipSlug = "resident-evil";
        when(ipLicenseRepository.findBySlugIgnoreCaseAndActiveTrue(ipSlug)).thenReturn(Optional.empty());

        ProvisionMachineCommand command = new ProvisionMachineCommand(
                "WP-123", ipSlug, "Resident Evil", UUID.randomUUID(), "admin-123"
        );

        ProvisionMachineResult result = sut.handle(command);

        assertThat(result).isInstanceOf(ProvisionMachineResult.UnknownIp.class);
        ProvisionMachineResult.UnknownIp unknownIp = (ProvisionMachineResult.UnknownIp) result;
        assertThat(unknownIp.ipSlug()).isEqualTo(ipSlug);

        verify(ipLicenseRepository).findBySlugIgnoreCaseAndActiveTrue(ipSlug);
        verifyNoInteractions(machineRepository);
        verifyNoInteractions(enrollmentTokenRepository);
    }

    @Test
    @DisplayName("Returns DuplicateSerial when machine serial number already exists")
    void handle_whenSerialAlreadyExists_shouldReturnDuplicateSerial() {
        String serialNumber = "WP-123";
        String ipSlug = "resident-evil";
        UUID idempotencyKey = UUID.randomUUID();

        IpLicense activeIp = new IpLicense("Resident Evil", ipSlug, true, UUID.randomUUID());
        when(ipLicenseRepository.findBySlugIgnoreCaseAndActiveTrue(ipSlug)).thenReturn(Optional.of(activeIp));
        when(machineRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(machineRepository.existsByWorldPinballSerialNumber(serialNumber)).thenReturn(true);

        ProvisionMachineCommand command = new ProvisionMachineCommand(
                serialNumber, ipSlug, "Resident Evil", idempotencyKey, "admin-123"
        );

        ProvisionMachineResult result = sut.handle(command);

        assertThat(result).isInstanceOf(ProvisionMachineResult.DuplicateSerial.class);
        ProvisionMachineResult.DuplicateSerial duplicate = (ProvisionMachineResult.DuplicateSerial) result;
        assertThat(duplicate.serialNumber()).isEqualTo(serialNumber);

        verify(machineRepository, never()).saveAndFlush(any());
        verifyNoInteractions(enrollmentTokenRepository);
    }

    @Test
    @DisplayName("Returns SameIdempotencyKey when idempotency key is already registered")
    void handle_whenIdempotencyKeyIsSame_shouldReturnSameIdempotencyKey() {
        String serialNumber = "WP-123";
        String ipSlug = "resident-evil";
        UUID idempotencyKey = UUID.randomUUID();

        IpLicense activeIp = new IpLicense("Resident Evil", ipSlug, true, UUID.randomUUID());
        when(ipLicenseRepository.findBySlugIgnoreCaseAndActiveTrue(ipSlug)).thenReturn(Optional.of(activeIp));
        when(machineRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);

        ProvisionMachineCommand command = new ProvisionMachineCommand(
                serialNumber, ipSlug, "Resident Evil", idempotencyKey, "admin-123"
        );

        ProvisionMachineResult result = sut.handle(command);

        assertThat(result).isInstanceOf(ProvisionMachineResult.SameIdempotencyKey.class);
        ProvisionMachineResult.SameIdempotencyKey sameKey = (ProvisionMachineResult.SameIdempotencyKey) result;
        assertThat(sameKey.idempotencyKey()).isEqualTo(idempotencyKey);

        verify(machineRepository, never()).saveAndFlush(any());
        verifyNoInteractions(enrollmentTokenRepository);
    }

    @Test
    @DisplayName("Provisions machine, creates enrollment token and returns Success on valid command")
    void handle_whenValidCommand_shouldReturnSuccess() {
        String serialNumber = "WP-123";
        String ipSlug = "resident-evil";
        UUID idempotencyKey = UUID.randomUUID();
        String plainToken = "plain-enrollment-token-base64";
        String tokenHash = "token-hash-hex";

        IpLicense activeIp = new IpLicense("Resident Evil", ipSlug, true, UUID.randomUUID());
        when(ipLicenseRepository.findBySlugIgnoreCaseAndActiveTrue(ipSlug)).thenReturn(Optional.of(activeIp));
        when(machineRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(machineRepository.existsByWorldPinballSerialNumber(serialNumber)).thenReturn(false);

        when(secretService.generate()).thenReturn(new SecretGenerationResult(plainToken, tokenHash, "encrypted-payload"));

        ProvisionMachineCommand command = new ProvisionMachineCommand(
                serialNumber, ipSlug, "Resident Evil", idempotencyKey, "admin-123"
        );

        ProvisionMachineResult result = sut.handle(command);

        assertThat(result).isInstanceOf(ProvisionMachineResult.Success.class);
        ProvisionMachineResult.Success success = (ProvisionMachineResult.Success) result;
        assertThat(success.machineId()).isNotNull();
        assertThat(success.enrollmentToken()).isEqualTo(plainToken);

        verify(machineRepository).saveAndFlush(any(Machine.class));
        verify(enrollmentTokenRepository).save(any(EnrollmentToken.class));
    }
}

package com.worldpinball.leaderboard.application.features.machines.Admin;

import com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity.GetMachineSecurityHandlerImpl;
import com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity.GetMachineSecurityResult;
import com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity.queries.GetMachineSecurityQuery;
import com.worldpinball.leaderboard.domain.entities.CaCertificate;
import com.worldpinball.leaderboard.domain.entities.Device;
import com.worldpinball.leaderboard.domain.entities.DeviceCertificate;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.entities.MachineDevice;
import com.worldpinball.leaderboard.domain.enums.CaType;
import com.worldpinball.leaderboard.infrastructure.DeviceCertificateRepository;
import com.worldpinball.leaderboard.infrastructure.DeviceRepository;
import com.worldpinball.leaderboard.infrastructure.MachineDeviceRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMachineSecurityHandler Tests")
class GetMachineSecurityHandlerTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private MachineDeviceRepository machineDeviceRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceCertificateRepository deviceCertificateRepository;

    private GetMachineSecurityHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new GetMachineSecurityHandlerImpl(
                machineRepository,
                machineDeviceRepository,
                deviceRepository,
                deviceCertificateRepository
        );
    }

    @Test
    @DisplayName("Returns MachineNotFound when machine does not exist")
    void handle_whenMachineDoesNotExist_shouldReturnMachineNotFound() {
        UUID machineId = UUID.randomUUID();
        when(machineRepository.findById(machineId)).thenReturn(Optional.empty());

        GetMachineSecurityResult result = sut.handle(new GetMachineSecurityQuery(machineId));

        assertThat(result).isInstanceOf(GetMachineSecurityResult.MachineNotFound.class);
    }

    @Test
    @DisplayName("Returns Success with device and certificate details when machine is enrolled")
    void handle_whenMachineAndDeviceAndCertExist_shouldReturnSuccessWithFullDetails() {
        Instant now = Instant.now();
        Machine machine = new Machine(
                UUID.randomUUID(),
                "WP-00001",
                "Godzilla #1",
                now,
                UUID.randomUUID(),
                "nonce-1",
                UUID.randomUUID()
        );
        machine.completeEnrollment(now);
        UUID machineId = machine.getMachineId();

        Device device = Device.createActive(
                "DEVICE-00001",
                "-----BEGIN PUBLIC KEY-----\nTEST\n-----END PUBLIC KEY-----",
                "ecdsa-p256",
                now,
                UUID.randomUUID()
        );

        MachineDevice link = new MachineDevice(machineId, device.getDeviceId(), now);

        CaCertificate ca = new CaCertificate(
                CaType.Intermediate,
                "CN=WorldPinball CA",
                "CN=WorldPinball Root CA",
                "CA-0001",
                "CA-THUMB",
                "-----BEGIN CERTIFICATE-----",
                now.minusSeconds(3600),
                now.plusSeconds(86400),
                now,
                true,
                true,
                "Intermediate CA"
        );

        DeviceCertificate cert = new DeviceCertificate(
                device.getDeviceId(),
                ca.getCaId(),
                "CERT-00001",
                "CERT-THUMB",
                "CN=DEVICE-00001",
                ca.getSubjectDn(),
                "-----BEGIN CERTIFICATE-----\nDEV\n-----END CERTIFICATE-----",
                now,
                now.plusSeconds(86400),
                now
        );

        when(machineRepository.findById(machineId)).thenReturn(Optional.of(machine));
        when(machineDeviceRepository.findFirstByMachineIdAndActiveTrue(machineId)).thenReturn(Optional.of(link));
        when(deviceRepository.findById(device.getDeviceId())).thenReturn(Optional.of(device));
        when(deviceCertificateRepository.findFirstByDeviceIdOrderByIssuedAtUtcDesc(device.getDeviceId()))
                .thenReturn(Optional.of(cert));

        GetMachineSecurityResult result = sut.handle(new GetMachineSecurityQuery(machineId));

        assertThat(result).isInstanceOf(GetMachineSecurityResult.Success.class);
        GetMachineSecurityResult.Success success = (GetMachineSecurityResult.Success) result;
        assertThat(success.security().machineId()).isEqualTo(machineId);
        assertThat(success.security().devicePhysicalSerial()).isEqualTo("DEVICE-00001");
        assertThat(success.security().certSerialNumber()).isEqualTo("CERT-00001");
        assertThat(success.security().certStatus()).isEqualTo(0);
    }
}

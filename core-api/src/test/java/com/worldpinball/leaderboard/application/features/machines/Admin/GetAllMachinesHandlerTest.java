package com.worldpinball.leaderboard.application.features.machines.Admin;

import com.worldpinball.leaderboard.application.features.machines.Admin.getallmachines.GetAllMachinesHandlerImpl;
import com.worldpinball.leaderboard.application.features.machines.Admin.getallmachines.GetAllMachinesQuery;
import com.worldpinball.leaderboard.application.features.machines.Admin.getallmachines.GetAllMachinesResult;
import com.worldpinball.leaderboard.domain.entities.IpLicense;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import com.worldpinball.leaderboard.infrastructure.DeviceCertificateRepository;
import com.worldpinball.leaderboard.infrastructure.IpLicenseRepository;
import com.worldpinball.leaderboard.infrastructure.MachineDeviceRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAllMachinesHandler Tests")
class GetAllMachinesHandlerTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private IpLicenseRepository ipLicenseRepository;

    @Mock
    private MachineDeviceRepository machineDeviceRepository;

    @Mock
    private DeviceCertificateRepository deviceCertificateRepository;

    private GetAllMachinesHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new GetAllMachinesHandlerImpl(
                machineRepository,
                ipLicenseRepository,
                machineDeviceRepository,
                deviceCertificateRepository
        );
    }

    @Test
    @DisplayName("Returns Success with list of machines when machines exist")
    void handle_whenMachinesExist_shouldReturnSuccessWithListOfMachineRowDto() {
        UUID ipId1 = UUID.randomUUID();
        UUID ipId2 = UUID.randomUUID();

        Machine machine1 = new Machine(
                ipId1, "WP-SN-001", "MM-01", Instant.now(), UUID.randomUUID(), "nonce-1", null
        );
        machine1.completeEnrollment(Instant.now());

        Machine machine2 = new Machine(
                ipId2, "WP-SN-002", "AFM-01", Instant.now(), UUID.randomUUID(), "nonce-2", null
        );

        when(machineRepository.findAll()).thenReturn(List.of(machine1, machine2));

        IpLicense ip1 = new IpLicense("Medieval Madness", "mm", true, ipId1);
        IpLicense ip2 = new IpLicense("Attack from Mars", "afm", true, ipId2);
        when(ipLicenseRepository.findAllById(anySet())).thenReturn(List.of(ip1, ip2));

        GetAllMachinesResult result = sut.handle(new GetAllMachinesQuery());

        assertThat(result).isInstanceOf(GetAllMachinesResult.Success.class);
        GetAllMachinesResult.Success success = (GetAllMachinesResult.Success) result;
        assertThat(success.machines()).hasSize(2);
        assertThat(success.machines().get(0).machineStatus()).isEqualTo(MachineStatus.ACTIVE);
        assertThat(success.machines().get(1).machineStatus()).isEqualTo(MachineStatus.PENDING_ENROLLMENT);
    }

    @Test
    @DisplayName("Returns NotFound when repository contains no machines")
    void handle_whenNoMachinesExist_shouldReturnNotFound() {
        when(machineRepository.findAll()).thenReturn(List.of());

        GetAllMachinesResult result = sut.handle(new GetAllMachinesQuery());

        assertThat(result).isInstanceOf(GetAllMachinesResult.NotFound.class);
    }
}

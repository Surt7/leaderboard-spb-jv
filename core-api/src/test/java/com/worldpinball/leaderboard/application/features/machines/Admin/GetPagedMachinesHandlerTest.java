package com.worldpinball.leaderboard.application.features.machines.Admin;

import com.worldpinball.leaderboard.application.features.machines.Admin.getpagedmachines.GetPagedMachinesHandlerImpl;
import com.worldpinball.leaderboard.application.features.machines.Admin.getpagedmachines.GetPagedMachinesResult;
import com.worldpinball.leaderboard.application.features.machines.Admin.getpagedmachines.queries.GetPagedMachinesQuery;
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
@DisplayName("GetPagedMachinesHandler Tests")
class GetPagedMachinesHandlerTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private IpLicenseRepository ipLicenseRepository;

    @Mock
    private MachineDeviceRepository machineDeviceRepository;

    @Mock
    private DeviceCertificateRepository deviceCertificateRepository;

    private GetPagedMachinesHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new GetPagedMachinesHandlerImpl(
                machineRepository,
                ipLicenseRepository,
                machineDeviceRepository,
                deviceCertificateRepository
        );
    }

    @Test
    @DisplayName("Returns paged machines and fleet stats correctly")
    void handle_whenMachinesExist_shouldReturnPagedResponseWithFleetStats() {
        Instant now = Instant.now();
        UUID ipId = UUID.randomUUID();
        IpLicense ip = new IpLicense("Godzilla", "godzilla", true, ipId);

        Machine m1 = new Machine(ipId, "WP-00001", "Godzilla #1", now, UUID.randomUUID(), "n1", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        m1.completeEnrollment(now);

        Machine m2 = new Machine(ipId, "WP-00002", "Godzilla #2", now, UUID.randomUUID(), "n2", UUID.fromString("00000000-0000-0000-0000-000000000002"));
        m2.setStatus(MachineStatus.MAINTENANCE, now);

        Machine m3 = new Machine(ipId, "WP-00003", "Godzilla #3", now, UUID.randomUUID(), "n3", UUID.fromString("00000000-0000-0000-0000-000000000003"));

        when(machineRepository.findAll()).thenReturn(List.of(m1, m2, m3));
        when(ipLicenseRepository.findAllById(anySet())).thenReturn(List.of(ip));

        GetPagedMachinesResult result = sut.handle(new GetPagedMachinesQuery(2, null, 1, null));

        assertThat(result).isInstanceOf(GetPagedMachinesResult.Success.class);
        var page = ((GetPagedMachinesResult.Success) result).page();
        assertThat(page.items()).hasSize(2);
        assertThat(page.totalCount()).isEqualTo(3);
        assertThat(page.hasNextPage()).isTrue();
        assertThat(page.hasPreviousPage()).isFalse();
        assertThat(page.stats().total()).isEqualTo(3);
        assertThat(page.stats().online()).isEqualTo(1);
        assertThat(page.stats().maintenance()).isEqualTo(1);
        assertThat(page.stats().pending()).isEqualTo(1);
    }
}

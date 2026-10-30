package com.worldpinball.leaderboard.api.controllers.Admin;

import com.worldpinball.leaderboard.application.dtos.FleetStatsDto;
import com.worldpinball.leaderboard.application.dtos.PagedResponse;
import com.worldpinball.leaderboard.application.features.leaderboard.Admin.dashboard.GetMachineDashboardHandler;
import com.worldpinball.leaderboard.application.features.leaderboard.Admin.dashboard.GetMachineDashboardQuery;
import com.worldpinball.leaderboard.application.features.leaderboard.Admin.dashboard.GetMachineDashboardResult;
import com.worldpinball.leaderboard.application.features.leaderboard.Admin.dashboard.MachineDashboardDto;
import com.worldpinball.leaderboard.application.features.machines.Admin.getallmachines.GetAllMachinesHandler;
import com.worldpinball.leaderboard.application.features.machines.Admin.getallmachines.GetAllMachinesQuery;
import com.worldpinball.leaderboard.application.features.machines.Admin.getallmachines.GetAllMachinesResult;
import com.worldpinball.leaderboard.application.features.machines.Admin.getallmachines.MachineRowDto;
import com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity.GetMachineSecurityHandler;
import com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity.GetMachineSecurityResult;
import com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity.dtos.MachineSecurityDto;
import com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity.queries.GetMachineSecurityQuery;
import com.worldpinball.leaderboard.application.features.machines.Admin.getpagedmachines.GetPagedMachinesHandler;
import com.worldpinball.leaderboard.application.features.machines.Admin.getpagedmachines.GetPagedMachinesResult;
import com.worldpinball.leaderboard.application.features.machines.Admin.getpagedmachines.queries.GetPagedMachinesQuery;
import com.worldpinball.leaderboard.application.features.machines.Admin.provision.ProvisionMachineHandler;
import com.worldpinball.leaderboard.application.features.machines.Admin.provision.ProvisionMachineResult;
import com.worldpinball.leaderboard.application.features.machines.Admin.provision.commands.ProvisionMachineCommand;
import com.worldpinball.leaderboard.application.features.machines.Admin.provision.dtos.ProvisionMachineRequest;
import com.worldpinball.leaderboard.application.features.machines.Admin.provision.dtos.ProvisionMachineResponse;
import com.worldpinball.leaderboard.application.features.machines.Admin.reissuetoken.ReissueEnrollmentTokenHandler;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminMachinesController Tests")
class AdminMachinesControllerTest {

    @Mock
    private GetAllMachinesHandler getAllMachinesHandler;

    @Mock
    private ProvisionMachineHandler provisionMachineHandler;

    @Mock
    private ReissueEnrollmentTokenHandler reissueEnrollmentTokenHandler;

    @Mock
    private GetMachineDashboardHandler getMachineDashboardHandler;

    @Mock
    private GetMachineSecurityHandler getMachineSecurityHandler;

    @Mock
    private GetPagedMachinesHandler getPagedMachinesHandler;

    private AdminMachinesController sut;

    @BeforeEach
    void setUp() {
        sut = new AdminMachinesController(
                getAllMachinesHandler,
                provisionMachineHandler,
                reissueEnrollmentTokenHandler,
                getMachineDashboardHandler,
                getMachineSecurityHandler,
                getPagedMachinesHandler
        );
    }

    @Test
    @DisplayName("Provision returns 200 OK with ProvisionMachineResponse when handler succeeds")
    void provision_whenHandlerReturnsSuccess_shouldReturnOk() {
        UUID expectedMachineId = UUID.randomUUID();
        String expectedEnrollmentToken = "enrollment-token";

        when(provisionMachineHandler.handle(any(ProvisionMachineCommand.class)))
                .thenReturn(new ProvisionMachineResult.Success(expectedMachineId, expectedEnrollmentToken));

        ProvisionMachineRequest request = new ProvisionMachineRequest(
                "WP-123",
                "resident-evil",
                "Resident Evil"
        );
        Principal principal = () -> "test-admin-id";

        ResponseEntity<?> response = sut.provision(request, UUID.randomUUID().toString(), principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ProvisionMachineResponse body = (ProvisionMachineResponse) response.getBody();
        assertThat(body.machineId()).isEqualTo(expectedMachineId);
        assertThat(body.enrollmentToken()).isEqualTo(expectedEnrollmentToken);
    }

    @Test
    @DisplayName("GetDashboard returns 200 OK with dashboard stats when machine exists")
    void getDashboard_whenMachineExists_shouldReturnOkWithDashboardDto() {
        UUID machineId = UUID.randomUUID();
        MachineDashboardDto expectedDto = new MachineDashboardDto(
                machineId,
                "TZ-01",
                42,
                1_500_000L,
                250_000L,
                10,
                Instant.now()
        );

        when(getMachineDashboardHandler.handle(any(GetMachineDashboardQuery.class)))
                .thenReturn(new GetMachineDashboardResult.Success(expectedDto));

        ResponseEntity<?> response = sut.getDashboard(machineId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedDto);
    }

    @Test
    @DisplayName("GetDashboard returns 404 NOT_FOUND when machine not found")
    void getDashboard_whenMachineNotFound_shouldReturnNotFound() {
        UUID machineId = UUID.randomUUID();

        when(getMachineDashboardHandler.handle(any(GetMachineDashboardQuery.class)))
                .thenReturn(new GetMachineDashboardResult.NotFound(machineId));

        ResponseEntity<?> response = sut.getDashboard(machineId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GetAllMachines returns 200 OK with list of machines when called")
    void getAllMachines_whenCalled_shouldReturnOkWithListOfMachines() {
        List<MachineRowDto> machines = List.of(
                new MachineRowDto(
                        UUID.randomUUID(),
                        "Medieval Madness",
                        "MM-01",
                        MachineStatus.ACTIVE,
                        "Main hall",
                        Instant.now(),
                        null,
                        "WP-SN-001"
                )
        );

        when(getAllMachinesHandler.handle(any(GetAllMachinesQuery.class)))
                .thenReturn(new GetAllMachinesResult.Success(machines));

        ResponseEntity<?> response = sut.getAllMachines();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(machines);
    }

    @Test
    @DisplayName("GetAllMachines returns 404 NOT_FOUND when no machines found")
    void getAllMachines_whenNoMachinesFound_shouldReturnNotFound() {
        when(getAllMachinesHandler.handle(any(GetAllMachinesQuery.class)))
                .thenReturn(new GetAllMachinesResult.NotFound());

        ResponseEntity<?> response = sut.getAllMachines();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GetMachineSecurity returns 200 OK when machine exists")
    void getMachineSecurity_whenMachineExists_shouldReturnOk() {
        UUID machineId = UUID.randomUUID();
        MachineSecurityDto dto = new MachineSecurityDto(
                machineId,
                "WP-00001",
                "Godzilla #1",
                1,
                true,
                UUID.randomUUID(),
                "DEVICE-00001",
                true,
                "CERT-00001",
                "THUMBPRINT",
                "CN=DEVICE-00001",
                "CN=WorldPinball CA",
                Instant.now(),
                Instant.now().plusSeconds(86400),
                1,
                false,
                true,
                "Active"
        );

        when(getMachineSecurityHandler.handle(any(GetMachineSecurityQuery.class)))
                .thenReturn(new GetMachineSecurityResult.Success(dto));

        ResponseEntity<?> response = sut.getMachineSecurity(machineId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("GetMachineSecurity returns 404 NOT_FOUND when machine does not exist")
    void getMachineSecurity_whenMachineNotFound_shouldReturnNotFound() {
        UUID machineId = UUID.randomUUID();
        when(getMachineSecurityHandler.handle(any(GetMachineSecurityQuery.class)))
                .thenReturn(new GetMachineSecurityResult.MachineNotFound(machineId));

        ResponseEntity<?> response = sut.getMachineSecurity(machineId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GetPagedMachines returns 200 OK with PagedResponse when handler succeeds")
    void getPagedMachines_whenCalled_shouldReturnOkWithPagedResponse() {
        PagedResponse<MachineRowDto> page = new PagedResponse<>(
                List.of(),
                10,
                0,
                1,
                false,
                false,
                null,
                new FleetStatsDto(0, 0, 0, 0, 0)
        );
        when(getPagedMachinesHandler.handle(any(GetPagedMachinesQuery.class)))
                .thenReturn(new GetPagedMachinesResult.Success(page));

        ResponseEntity<?> response = sut.getPagedMachines(10, null, 1, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
    }
}

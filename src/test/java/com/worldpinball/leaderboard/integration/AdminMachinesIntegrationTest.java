package com.worldpinball.leaderboard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldpinball.leaderboard.application.features.machines.Admin.provision.dtos.ProvisionMachineRequest;
import com.worldpinball.leaderboard.domain.entities.IpLicense;
import com.worldpinball.leaderboard.domain.entities.Publisher;
import com.worldpinball.leaderboard.infrastructure.IpLicenseRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import com.worldpinball.leaderboard.infrastructure.PublisherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class AdminMachinesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IpLicenseRepository ipLicenseRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private MachineRepository machineRepository;

    private IpLicense ipLicense;

    @BeforeEach
    void setUp() {
        Publisher publisher = new Publisher("Stern Pinball", "stern", Instant.now());
        publisherRepository.save(publisher);

        ipLicense = new IpLicense(
                "Godzilla",
                "godzilla",
                publisher.getPublisherId(),
                true,
                Instant.now(),
                UUID.randomUUID()
        );
        ipLicenseRepository.save(ipLicense);
    }

    @Test
    @DisplayName("Provision machine successfully and list it")
    void shouldProvisionMachineSuccessfully() throws Exception {
        ProvisionMachineRequest req = new ProvisionMachineRequest(
                "WP-GODZILLA-001",
                "godzilla",
                "Godzilla Pro #1"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/admin/machines/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.machineId").isNotEmpty())
                .andExpect(jsonPath("$.enrollmentToken").isNotEmpty())
                .andReturn();

        // Check GET all machines
        mockMvc.perform(get("/api/v1/admin/machines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[?(@.serialNumber == 'WP-GODZILLA-001')].displayName").value("Godzilla Pro #1"));
    }

    @Test
    @DisplayName("Provision machine with unknown IP returns 404")
    void shouldReturn404WhenIpUnknown() throws Exception {
        ProvisionMachineRequest req = new ProvisionMachineRequest(
                "WP-UNKNOWN-001",
                "unknown-table",
                "Unknown Table"
        );

        mockMvc.perform(post("/api/v1/admin/machines/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Unknown or inactive Ip slug")));
    }

    @Test
    @DisplayName("Provision machine with duplicate serial returns 409")
    void shouldReturn409WhenDuplicateSerial() throws Exception {
        ProvisionMachineRequest req1 = new ProvisionMachineRequest(
                "WP-GODZILLA-DUP",
                "godzilla",
                "Godzilla Pro First"
        );
        mockMvc.perform(post("/api/v1/admin/machines/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        ProvisionMachineRequest req2 = new ProvisionMachineRequest(
                "WP-GODZILLA-DUP",
                "godzilla",
                "Godzilla Pro Second"
        );
        mockMvc.perform(post("/api/v1/admin/machines/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Already existing serial number")));
    }

    @Test
    @DisplayName("Reissue enrollment token for machine")
    void shouldReissueTokenSuccessfully() throws Exception {
        ProvisionMachineRequest req = new ProvisionMachineRequest(
                "WP-GODZILLA-REISSUE",
                "godzilla",
                "Godzilla Pro Reissue"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/admin/machines/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        String machineId = (String) map.get("machineId");

        mockMvc.perform(post("/api/v1/admin/machines/" + machineId + "/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.machineId").value(machineId))
                .andExpect(jsonPath("$.enrollmentToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresAtUtc").isNotEmpty());
    }

    @Test
    @DisplayName("Retrieve machine dashboard stats")
    void shouldRetrieveMachineDashboard() throws Exception {
        ProvisionMachineRequest req = new ProvisionMachineRequest(
                "WP-GODZILLA-DASH",
                "godzilla",
                "Godzilla Pro Dash"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/admin/machines/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        String machineId = (String) map.get("machineId");

        mockMvc.perform(get("/api/v1/admin/machines/" + machineId + "/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.machineId").value(machineId))
                .andExpect(jsonPath("$.serialNumber").value("WP-GODZILLA-DASH"))
                .andExpect(jsonPath("$.totalGamesPlayed").value(0))
                .andExpect(jsonPath("$.highScore").value(0));
    }
}

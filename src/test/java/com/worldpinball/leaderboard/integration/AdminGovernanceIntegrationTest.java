package com.worldpinball.leaderboard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldpinball.leaderboard.application.features.machines.Admin.createoperatortoken.CreateOperatorTokenRequest;
import com.worldpinball.leaderboard.application.features.machines.Admin.provision.dtos.ProvisionMachineRequest;
import com.worldpinball.leaderboard.application.features.machines.Public.enroll.EnrollDeviceRequest;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.createstreamaccessgrant.CreateStreamAccessGrantRequest;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant.RevokeStreamAccessGrantRequest;
import com.worldpinball.leaderboard.domain.entities.IpLicense;
import com.worldpinball.leaderboard.domain.entities.Publisher;
import com.worldpinball.leaderboard.domain.enums.GrantOrigin;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import com.worldpinball.leaderboard.domain.enums.PrincipalType;
import com.worldpinball.leaderboard.infrastructure.IpLicenseRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import com.worldpinball.leaderboard.infrastructure.PublisherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AdminGovernanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private IpLicenseRepository ipLicenseRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Test
    void shouldCreateOperatorToken() throws Exception {
        CreateOperatorTokenRequest request = new CreateOperatorTokenRequest(
                "Factory Line #1 Technician",
                "Operator:Enroll",
                7
        );

        mockMvc.perform(post("/api/v1/admin/operator-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenId").isNotEmpty())
                .andExpect(jsonPath("$.rawToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresAtUtc").isNotEmpty());
    }

    @Test
    void shouldCreateListAndRevokeStreamingAccessGrant() throws Exception {
        UUID principalId = UUID.randomUUID();
        CreateStreamAccessGrantRequest createReq = new CreateStreamAccessGrantRequest(
                PrincipalType.USER,
                principalId,
                "Twitch Caster Finals",
                GrantOrigin.WHITELIST,
                Instant.now().plus(24, ChronoUnit.HOURS)
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/streaming-access/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grantId").isNotEmpty())
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID grantId = UUID.fromString(createdJson.get("grantId").asText());

        // List grants
        mockMvc.perform(get("/api/v1/admin/streaming-access/grants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grantId").isNotEmpty());

        // Revoke grant
        RevokeStreamAccessGrantRequest revokeReq = new RevokeStreamAccessGrantRequest("Tournament ended");
        mockMvc.perform(post("/api/v1/admin/streaming-access/grants/{grantId}/revoke", grantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));

        // Revoking again returns 409 Conflict
        mockMvc.perform(post("/api/v1/admin/streaming-access/grants/{grantId}/revoke", grantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeReq)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldProvisionMachineAndEnrollPhysicalDevice() throws Exception {
        Instant now = Instant.now();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Publisher publisher = new Publisher("Williams " + suffix, "williams-" + suffix, now);
        publisherRepository.save(publisher);

        String ipSlug = "mm-" + suffix;
        IpLicense ip = new IpLicense("Medieval Madness " + suffix, ipSlug, publisher.getPublisherId(), true, now, UUID.randomUUID());
        ipLicenseRepository.save(ip);

        // 1. Provision Machine (Admin)
        ProvisionMachineRequest provisionReq = new ProvisionMachineRequest(
                "WP-ENROLL-" + suffix,
                ipSlug,
                "Medieval Madness LE #" + suffix
        );

        MvcResult provisionRes = mockMvc.perform(post("/api/v1/admin/machines/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisionReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.machineId").isNotEmpty())
                .andExpect(jsonPath("$.enrollmentToken").isNotEmpty())
                .andReturn();

        JsonNode provisionJson = objectMapper.readTree(provisionRes.getResponse().getContentAsString());
        UUID machineId = UUID.fromString(provisionJson.get("machineId").asText());
        String plainEnrollmentToken = provisionJson.get("enrollmentToken").asText();

        // 2. Enroll physical device using the enrollment token (Public)
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("EC");
        kpg.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
        String validEcdsaPem = "-----BEGIN PUBLIC KEY-----\n"
                + java.util.Base64.getEncoder().encodeToString(kpg.generateKeyPair().getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
        EnrollDeviceRequest enrollReq = new EnrollDeviceRequest(
                machineId,
                plainEnrollmentToken,
                validEcdsaPem,
                "DEV-SN-" + suffix
        );

        mockMvc.perform(post("/api/v1/machines/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").isNotEmpty())
                .andExpect(jsonPath("$.machineId").value(machineId.toString()))
                .andExpect(jsonPath("$.certPem").isNotEmpty())
                .andExpect(jsonPath("$.certSerialNumber").isNotEmpty());

        // Verify machine transitioned to ACTIVE
        var updatedMachine = machineRepository.findById(machineId).orElseThrow();
        assertThat(updatedMachine.getStatus()).isEqualTo(MachineStatus.ACTIVE);
    }

    @Test
    void shouldVerifyTestEndpoints() throws Exception {
        mockMvc.perform(get("/api/test/auth"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test/admin"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test/whoami"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").isNotEmpty())
                .andExpect(jsonPath("$.email").isNotEmpty());
    }
}

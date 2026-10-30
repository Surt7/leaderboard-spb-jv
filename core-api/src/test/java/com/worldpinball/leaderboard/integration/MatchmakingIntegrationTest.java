package com.worldpinball.leaderboard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue.JoinMatchmakingQueueRequest;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue.LeaveMatchmakingQueueRequest;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.matchnextplayer.MatchNextPlayerCommand;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.matchnextplayer.MatchNextPlayerHandler;
import com.worldpinball.leaderboard.application.features.matchmaking.Public.matchnextplayer.MatchNextPlayerResult;
import com.worldpinball.leaderboard.domain.entities.*;
import com.worldpinball.leaderboard.domain.enums.TelemetryMode;
import com.worldpinball.leaderboard.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatchmakingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private IpLicenseRepository ipLicenseRepository;

    @Autowired
    private GameModeRepository gameModeRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private MatchNextPlayerHandler matchNextPlayerHandler;

    private Machine machine;
    private GameSession session;
    private final UUID hostUserId = UUID.randomUUID();
    private final UUID queuedPlayerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userProfileRepository.save(new UserProfile(
                hostUserId, "Host Player", "HST", null, "FR", true, Instant.now()
        ));
        userProfileRepository.save(new UserProfile(
                queuedPlayerId, "Queued Player", "QUD", null, "US", true, Instant.now()
        ));

        Publisher publisher = new Publisher("Stern", "stern-pinball", Instant.now());
        publisherRepository.save(publisher);

        IpLicense ipLicense = new IpLicense("Jurassic Park", "jurassic-park", publisher.getPublisherId(), true, Instant.now(), UUID.randomUUID());
        ipLicenseRepository.save(ipLicense);

        GameMode mode = new GameMode("standard", "Standard Mode", "Standard mode", true, Instant.now(), UUID.randomUUID());
        gameModeRepository.save(mode);

        Device device = deviceRepository.save(Device.createActive(
                "SN-JP-001", "dummy-key", "RSA", Instant.now(), UUID.randomUUID()
        ));

        machine = new Machine(ipLicense.getIpId(), "WP-JP-001", "JP Premium", Instant.now(), UUID.randomUUID(), "nonce-jp", UUID.randomUUID());
        machineRepository.save(machine);

        session = new GameSession(
                machine.getMachineId(),
                device.getDeviceId(),
                mode.getModeId(),
                "1.0.0",
                "nonce-sess-1",
                Instant.now(),
                UUID.randomUUID()
        );
        session.addPlayer(1, hostUserId);
        session.searchPlayers();
        sessionRepository.save(session);
    }

    @Test
    @DisplayName("Player joins matchmaking queue and leaves successfully")
    void shouldJoinAndLeaveQueue() throws Exception {
        JoinMatchmakingQueueRequest joinReq = new JoinMatchmakingQueueRequest(machine.getMachineId(), queuedPlayerId);

        mockMvc.perform(post("/api/v1/matchmaking/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Player enqueued successfully."));

        LeaveMatchmakingQueueRequest leaveReq = new LeaveMatchmakingQueueRequest(machine.getMachineId(), queuedPlayerId);

        mockMvc.perform(post("/api/v1/matchmaking/leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leaveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Player removed from queue."));
    }

    @Test
    @DisplayName("Join with unknown machine returns 404")
    void shouldReturn404WhenMachineNotFound() throws Exception {
        JoinMatchmakingQueueRequest req = new JoinMatchmakingQueueRequest(UUID.randomUUID(), queuedPlayerId);

        mockMvc.perform(post("/api/v1/matchmaking/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Match next player from queue into active game session")
    void shouldMatchNextPlayerIntoSession() throws Exception {
        JoinMatchmakingQueueRequest joinReq = new JoinMatchmakingQueueRequest(machine.getMachineId(), queuedPlayerId);

        mockMvc.perform(post("/api/v1/matchmaking/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk());

        MatchNextPlayerResult result = matchNextPlayerHandler.handle(
                new MatchNextPlayerCommand(session.getSessionId(), machine.getMachineId())
        );

        assertThat(result).isInstanceOf(MatchNextPlayerResult.Success.class);
        MatchNextPlayerResult.Success success = (MatchNextPlayerResult.Success) result;
        assertThat(success.playerId()).isEqualTo(queuedPlayerId);
        assertThat(success.slotNumber()).isEqualTo(2);
        assertThat(success.matchFound()).isTrue();
    }
}

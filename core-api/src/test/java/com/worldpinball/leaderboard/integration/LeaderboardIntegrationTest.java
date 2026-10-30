package com.worldpinball.leaderboard.integration;

import com.worldpinball.leaderboard.application.features.leaderboard.Public.getleaderboard.GetLeaderboardHandler;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getleaderboard.GetLeaderboardQuery;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getleaderboard.GetLeaderboardResult;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getplayerrank.GetPlayerRankHandler;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getplayerrank.GetPlayerRankQuery;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getplayerrank.GetPlayerRankResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.SubmitScoreHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.SubmitScoreResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.commands.SubmitScoreCommand;
import com.worldpinball.leaderboard.domain.entities.*;
import com.worldpinball.leaderboard.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class LeaderboardIntegrationTest {

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
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private SubmitScoreHandler submitScoreHandler;

    @Autowired
    private GetLeaderboardHandler getLeaderboardHandler;

    @Autowired
    private GetPlayerRankHandler getPlayerRankHandler;

    private IpLicense ipLicense;
    private GameMode gameMode;
    private Device device;
    private Machine machine;
    private GameSession session;

    @BeforeEach
    void setUp() {
        Publisher publisher = publisherRepository.save(new Publisher(
            "Stern Pinball", "stern-" + UUID.randomUUID(), Instant.now()
        ));

        ipLicense = ipLicenseRepository.save(new IpLicense(
            "Godzilla", "godzilla-" + UUID.randomUUID(), publisher.getPublisherId(), true, Instant.now(), null
        ));

        gameMode = gameModeRepository.save(new GameMode(
            "std-" + UUID.randomUUID().toString().substring(0, 8), "Standard 3-Ball", "Standard 3-Ball Mode", true, Instant.now(), null
        ));

        device = deviceRepository.save(Device.createActive(
            "DEV-" + UUID.randomUUID().toString().substring(0, 8), "dummy-public-key", "RSA", Instant.now(), null
        ));

        Machine rawMachine = new Machine(
            ipLicense.getIpId(), "FLP-" + UUID.randomUUID().toString().substring(0, 8), "Godzilla Machine", Instant.now(), UUID.randomUUID(), "nonce-" + UUID.randomUUID(), null
        );
        rawMachine.completeEnrollment(Instant.now());
        machine = machineRepository.save(rawMachine);

        GameSession rawSession = new GameSession(
            machine.getMachineId(), device.getDeviceId(), gameMode.getModeId(), "1.0.0", "nonce-" + UUID.randomUUID(), Instant.now(), null
        );
        rawSession.addPlayer(1, UUID.randomUUID());
        rawSession.addPlayer(2, UUID.randomUUID());
        rawSession.launch(Instant.now());
        session = gameSessionRepository.save(rawSession);
    }

    @Test
    @DisplayName("Should submit scores via CQRS, prevent slot duplicates with Result pattern, and query rankings")
    void testLeaderboardCqrsWorkflow() {
        // 1. Soumettre un premier score (250M) sur le slot 1
        SubmitScoreCommand cmd1 = new SubmitScoreCommand(
            session.getSessionId(),
            1,
            device.getDeviceId(),
            machine.getMachineId(),
            ipLicense.getIpId(),
            gameMode.getModeId(),
            250_000_000L,
            Instant.now().minusSeconds(10),
            null
        );
        SubmitScoreResult result1 = submitScoreHandler.handle(cmd1);
        assertThat(result1).isInstanceOf(SubmitScoreResult.Success.class);

        // 2. Tenter de soumettre à nouveau sur le slot 1 (Result Pattern: SlotAlreadyOccupied)
        SubmitScoreResult duplicateResult = submitScoreHandler.handle(cmd1);
        assertThat(duplicateResult).isInstanceOf(SubmitScoreResult.SlotAlreadyOccupied.class);

        // 3. Soumettre un score supérieur (500M) sur le slot 2
        SubmitScoreCommand cmd2 = new SubmitScoreCommand(
            session.getSessionId(),
            2,
            device.getDeviceId(),
            machine.getMachineId(),
            ipLicense.getIpId(),
            gameMode.getModeId(),
            500_000_000L,
            Instant.now(),
            null
        );
        SubmitScoreResult result2 = submitScoreHandler.handle(cmd2);
        assertThat(result2).isInstanceOf(SubmitScoreResult.Success.class);

        // 4. Exécuter la Query GetLeaderboard (Top 10)
        GetLeaderboardQuery query = new GetLeaderboardQuery(
            ipLicense.getIpId(),
            gameMode.getModeId(),
            null,
            null,
            10
        );
        GetLeaderboardResult queryResult = getLeaderboardHandler.handle(query);
        assertThat(queryResult).isInstanceOf(GetLeaderboardResult.Success.class);

        var rows = ((GetLeaderboardResult.Success) queryResult).rows();
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).rank()).isEqualTo(1);
        assertThat(rows.get(0).score()).isEqualTo(500_000_000L);
        assertThat(rows.get(1).rank()).isEqualTo(2);
        assertThat(rows.get(1).score()).isEqualTo(250_000_000L);

        // 5. Exécuter la Query GetPlayerRank pour un score de 300M
        GetPlayerRankQuery rankQuery = new GetPlayerRankQuery(
            ipLicense.getIpId(),
            gameMode.getModeId(),
            300_000_000L
        );
        GetPlayerRankResult rankResult = getPlayerRankHandler.handle(rankQuery);
        assertThat(rankResult).isInstanceOf(GetPlayerRankResult.Success.class);

        long rank = ((GetPlayerRankResult.Success) rankResult).rank();
        assertThat(rank).isEqualTo(2); // Seul 500M est supérieur, donc rang 2
    }
}

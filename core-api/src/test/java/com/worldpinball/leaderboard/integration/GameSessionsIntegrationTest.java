package com.worldpinball.leaderboard.integration;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession.AbortSessionHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession.AbortSessionResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession.commands.AbortSessionCommand;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.AddPlayerHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.AddPlayerResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.commands.AddPlayerCommand;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession.CreateGameSessionHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession.CreateGameSessionResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.creategamesession.commands.CreateGameSessionCommand;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.GetLobbyHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.GetLobbyResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.queries.GetLobbyQuery;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer.KickPlayerHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer.KickPlayerResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer.commands.KickPlayerCommand;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession.LaunchSessionHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession.LaunchSessionResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession.commands.LaunchSessionCommand;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore.RecordBallScoreHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore.RecordBallScoreResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore.commands.RecordBallScoreCommand;
import com.worldpinball.leaderboard.domain.entities.*;
import com.worldpinball.leaderboard.domain.enums.GameSessionState;
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
class GameSessionsIntegrationTest {

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
    private MachineDeviceRepository machineDeviceRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private CreateGameSessionHandler createGameSessionHandler;

    @Autowired
    private GetLobbyHandler getLobbyHandler;

    @Autowired
    private AddPlayerHandler addPlayerHandler;

    @Autowired
    private KickPlayerHandler kickPlayerHandler;

    @Autowired
    private LaunchSessionHandler launchSessionHandler;

    @Autowired
    private AbortSessionHandler abortSessionHandler;

    @Autowired
    private RecordBallScoreHandler recordBallScoreHandler;

    private Device device;
    private Machine machine;
    private GameMode gameMode;

    @BeforeEach
    void setUp() {
        Publisher publisher = publisherRepository.save(new Publisher(
            "Stern Pinball", "stern-" + UUID.randomUUID(), Instant.now()
        ));

        IpLicense ipLicense = ipLicenseRepository.save(new IpLicense(
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

        machineDeviceRepository.save(new MachineDevice(
            machine.getMachineId(),
            device.getDeviceId(),
            Instant.now()
        ));
    }

    @Test
    @DisplayName("Complete GameSession lifecycle: Create -> Lobby -> AddPlayer -> KickPlayer -> Launch -> RecordBall -> Abort")
    void testCompleteGameSessionLifecycle() {
        // 1. Create Session
        CreateGameSessionCommand createCmd = new CreateGameSessionCommand(
            device.getDeviceId(),
            gameMode.getCode(),
            "2.4.0"
        );
        CreateGameSessionResult createResult = createGameSessionHandler.handle(createCmd);
        assertThat(createResult).isInstanceOf(CreateGameSessionResult.Success.class);

        var successCreate = (CreateGameSessionResult.Success) createResult;
        UUID sessionId = successCreate.sessionId();
        assertThat(successCreate.nonce()).hasSize(32);

        // 2. Get Lobby (initially 4 empty slots)
        GetLobbyResult lobbyResult1 = getLobbyHandler.handle(new GetLobbyQuery(sessionId));
        assertThat(lobbyResult1).isInstanceOf(GetLobbyResult.Success.class);
        var lobby1 = ((GetLobbyResult.Success) lobbyResult1).lobby();
        assertThat(lobby1.slots()).hasSize(4);
        assertThat(lobby1.slots()).allMatch(s -> !s.isOccupied());
        assertThat(lobby1.sessionState()).isEqualTo(GameSessionState.CREATED.name());

        // 3. Add Player 1 (Guest "TOM")
        AddPlayerResult addP1Result = addPlayerHandler.handle(new AddPlayerCommand(
            sessionId, 1, null, "TOM", false
        ));
        assertThat(addP1Result).isInstanceOf(AddPlayerResult.Success.class);

        // 4. Add Player 2 (Registered User)
        UUID user2Id = UUID.randomUUID();
        userProfileRepository.save(new UserProfile(
            user2Id, "PlayerTwo", "PL2", null, "FR", true, Instant.now()
        ));
        AddPlayerResult addP2Result = addPlayerHandler.handle(new AddPlayerCommand(
            sessionId, 2, user2Id, null, true
        ));
        assertThat(addP2Result).isInstanceOf(AddPlayerResult.Success.class);

        // 5. Duplicate slot collision check (Slot 1 already occupied)
        UUID user3Id = UUID.randomUUID();
        userProfileRepository.save(new UserProfile(
            user3Id, "PlayerThree", "PL3", null, "FR", true, Instant.now()
        ));
        AddPlayerResult duplicateSlotResult = addPlayerHandler.handle(new AddPlayerCommand(
            sessionId, 1, user3Id, null, true
        ));
        assertThat(duplicateSlotResult).isInstanceOf(AddPlayerResult.SlotAlreadyOccupied.class);

        // 6. Registered user requirement check
        AddPlayerResult missingUserResult = addPlayerHandler.handle(new AddPlayerCommand(
            sessionId, 3, null, "BOB", true
        ));
        assertThat(missingUserResult).isInstanceOf(AddPlayerResult.RegisteredUserRequired.class);

        // 7. Duplicate user check
        AddPlayerResult duplicateUserResult = addPlayerHandler.handle(new AddPlayerCommand(
            sessionId, 3, user2Id, null, true
        ));
        assertThat(duplicateUserResult).isInstanceOf(AddPlayerResult.UserAlreadyRegistered.class);

        // 8. Verify Lobby contains both players
        GetLobbyResult lobbyResult2 = getLobbyHandler.handle(new GetLobbyQuery(sessionId));
        var lobby2 = ((GetLobbyResult.Success) lobbyResult2).lobby();
        assertThat(lobby2.slots().get(0).isOccupied()).isTrue();
        assertThat(lobby2.slots().get(0).guestInitials()).isEqualTo("TOM");
        assertThat(lobby2.slots().get(1).isOccupied()).isTrue();
        assertThat(lobby2.slots().get(1).userId()).isEqualTo(user2Id);
        assertThat(lobby2.slots().get(2).isOccupied()).isFalse();

        // 9. Kick Player 2 from slot 2
        KickPlayerResult kickResult = kickPlayerHandler.handle(new KickPlayerCommand(sessionId, 2));
        assertThat(kickResult).isInstanceOf(KickPlayerResult.Success.class);

        // 10. Verify Slot 2 is free again
        GetLobbyResult lobbyResult3 = getLobbyHandler.handle(new GetLobbyQuery(sessionId));
        var lobby3 = ((GetLobbyResult.Success) lobbyResult3).lobby();
        assertThat(lobby3.slots().get(1).isOccupied()).isFalse();

        // 11. Add Player 2 back to slot 2
        AddPlayerResult addP2Again = addPlayerHandler.handle(new AddPlayerCommand(
            sessionId, 2, user2Id, null, true
        ));
        assertThat(addP2Again).isInstanceOf(AddPlayerResult.Success.class);

        // 12. Launch Session
        LaunchSessionResult launchResult = launchSessionHandler.handle(new LaunchSessionCommand(sessionId));
        assertThat(launchResult).isInstanceOf(LaunchSessionResult.Success.class);

        GameSession sessionAfterLaunch = gameSessionRepository.findById(sessionId).orElseThrow();
        assertThat(sessionAfterLaunch.getState()).isEqualTo(GameSessionState.STARTED);

        // 13. Record ball score for slot 1
        RecordBallScoreResult ballResult = recordBallScoreHandler.handle(new RecordBallScoreCommand(
            sessionId,
            device.getDeviceId(),
            1,
            1,
            1_500_000L
        ));
        assertThat(ballResult).isInstanceOf(RecordBallScoreResult.Success.class);

        // 14. Record ball score with device mismatch
        RecordBallScoreResult mismatchResult = recordBallScoreHandler.handle(new RecordBallScoreCommand(
            sessionId,
            UUID.randomUUID(), // rogue device
            1,
            2,
            500_000L
        ));
        assertThat(mismatchResult).isInstanceOf(RecordBallScoreResult.DeviceMismatch.class);

        // 15. Abort session
        AbortSessionResult abortResult = abortSessionHandler.handle(new AbortSessionCommand(sessionId));
        assertThat(abortResult).isInstanceOf(AbortSessionResult.Success.class);

        GameSession sessionAfterAbort = gameSessionRepository.findById(sessionId).orElseThrow();
        assertThat(sessionAfterAbort.getState()).isEqualTo(GameSessionState.ABORTED);
    }
}

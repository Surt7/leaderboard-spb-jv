package com.worldpinball.leaderboard.infrastructure.seed;

import com.worldpinball.leaderboard.application.services.RealtimeLeaderboardService;
import com.worldpinball.leaderboard.application.services.RolePermissionReloader;
import com.worldpinball.leaderboard.domain.entities.*;
import com.worldpinball.leaderboard.domain.enums.CaType;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import com.worldpinball.leaderboard.domain.enums.RevocationReason;
import com.worldpinball.leaderboard.infrastructure.*;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Mirror of C#'s DevelopmentDataSeeder (using Bogus).
 * Populates the development database with realistic test entities using Datafaker.
 * Activated strictly on the 'dev' profile.
 */
@Component
@Profile("dev")
public class DevelopmentDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentDataSeeder.class);
    private static final Faker faker = new Faker(new Random(20260903));

    public static final UUID PHILIPPE_USER_ID = UUID.fromString("a2c66552-2b07-42cb-9814-0f5abacfd902");
    public static final String DEV_OPERATOR_TOKEN = "wpt_dev_9f8e7d6c5b4a32109876543210fedcba";
    private static final String DEVELOPMENT_USER_ID = "development-seeder";

    private final PublisherRepository publisherRepository;
    private final GameModeRepository gameModeRepository;
    private final IpLicenseRepository ipLicenseRepository;
    private final MachineRepository machineRepository;
    private final DeviceRepository deviceRepository;
    private final MachineDeviceRepository machineDeviceRepository;
    private final CaCertificateRepository caCertificateRepository;
    private final DeviceCertificateRepository deviceCertificateRepository;
    private final CertificateRevocationRepository certificateRevocationRepository;
    private final OperatorTokenRepository operatorTokenRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserProfileRepository userProfileRepository;
    private final GameSessionRepository gameSessionRepository;
    private final ScoreEntryRepository scoreEntryRepository;
    private final RealtimeLeaderboardService realtimeLeaderboardService;
    private final RolePermissionReloader rolePermissionReloader;

    public DevelopmentDataSeeder(
            PublisherRepository publisherRepository,
            GameModeRepository gameModeRepository,
            IpLicenseRepository ipLicenseRepository,
            MachineRepository machineRepository,
            DeviceRepository deviceRepository,
            MachineDeviceRepository machineDeviceRepository,
            CaCertificateRepository caCertificateRepository,
            DeviceCertificateRepository deviceCertificateRepository,
            CertificateRevocationRepository certificateRevocationRepository,
            OperatorTokenRepository operatorTokenRepository,
            RolePermissionRepository rolePermissionRepository,
            UserProfileRepository userProfileRepository,
            GameSessionRepository gameSessionRepository,
            ScoreEntryRepository scoreEntryRepository,
            RealtimeLeaderboardService realtimeLeaderboardService,
            RolePermissionReloader rolePermissionReloader
    ) {
        this.publisherRepository = publisherRepository;
        this.gameModeRepository = gameModeRepository;
        this.ipLicenseRepository = ipLicenseRepository;
        this.machineRepository = machineRepository;
        this.deviceRepository = deviceRepository;
        this.machineDeviceRepository = machineDeviceRepository;
        this.caCertificateRepository = caCertificateRepository;
        this.deviceCertificateRepository = deviceCertificateRepository;
        this.certificateRevocationRepository = certificateRevocationRepository;
        this.operatorTokenRepository = operatorTokenRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userProfileRepository = userProfileRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.scoreEntryRepository = scoreEntryRepository;
        this.realtimeLeaderboardService = realtimeLeaderboardService;
        this.rolePermissionReloader = rolePermissionReloader;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed();
    }

    @Transactional
    public void seed() {
        Instant now = Instant.now();
        if (machineRepository.count() >= 20) {
            log.info("Development database already seeded. Ensuring certificates and permissions are up to date...");
            seedCertificatesAndPermissions(now, deviceRepository.findAll());
            return;
        }

        log.info("Starting Development database seeding (Datafaker)...");

        // 1. Publishers
        List<Publisher> publishers = publisherRepository.findAll();
        if (publishers.isEmpty()) {
            publishers = publisherRepository.saveAll(List.of(
                    new Publisher("Stern Pinball", "stern-pinball", now),
                    new Publisher("Williams Bally", "williams-bally", now),
                    new Publisher("Jersey Jack Pinball", "jersey-jack", now),
                    new Publisher("Spooky Pinball", "spooky-pinball", now),
                    new Publisher("Chicago Gaming", "chicago-gaming", now)
            ));
        }

        // 2. Game Modes
        List<GameMode> modes = gameModeRepository.findAll();
        if (modes.isEmpty()) {
            modes = gameModeRepository.saveAll(List.of(
                    new GameMode("mode-1", "Standard Mode", "Default game mode for test tools and devices", true, now, UUID.randomUUID()),
                    new GameMode("classic", "Classic Mode", "Standard 3-ball play", true, now, UUID.randomUUID()),
                    new GameMode("timed", "Timed Rush", "Score as many points in 2 minutes", true, now, UUID.randomUUID()),
                    new GameMode("multiball", "Multiball Madness", "Continuous multiball frenzy", true, now, UUID.randomUUID()),
                    new GameMode("wizard", "Wizard Challenge", "All wizard modes unlocked", true, now, UUID.randomUUID()),
                    new GameMode("tournament", "Tournament Mode", "No extra balls, strict tilt rules", true, now, UUID.randomUUID())
            ));
        }

        // 3. IP Licenses
        List<IpLicense> licenses = ipLicenseRepository.findAll();
        if (licenses.isEmpty()) {
            String[] titles = {
                    "Godzilla", "Jurassic Park", "Medieval Madness", "Attack from Mars",
                    "Deadpool", "Iron Maiden", "Stranger Things", "Monster Bash",
                    "The Mandalorian", "Foo Fighters"
            };
            List<IpLicense> newLicenses = new ArrayList<>();
            for (int i = 0; i < titles.length; i++) {
                Publisher pub = publishers.get(i % publishers.size());
                String slug = titles[i].toLowerCase().replaceAll("[^a-z0-9]", "-");
                newLicenses.add(new IpLicense(titles[i], slug, pub.getPublisherId(), true, now, UUID.randomUUID()));
            }
            licenses = ipLicenseRepository.saveAll(newLicenses);
        }

        // 4. User Profiles (Philippe + 12 players)
        List<UserProfile> users = userProfileRepository.findAll();
        UserProfile philippe = userProfileRepository.findById(PHILIPPE_USER_ID).orElse(null);
        if (philippe == null) {
            philippe = userProfileRepository.save(new UserProfile(
                    PHILIPPE_USER_ID,
                    "Philippe",
                    "PHI",
                    "",
                    "FR",
                    true,
                    now
            ));
        }
        if (users.size() < 13) {
            List<UserProfile> newUsers = new ArrayList<>();
            for (int i = 1; i <= 12; i++) {
                String name = faker.name().firstName() + " " + faker.name().lastName();
                String initials = faker.regexify("[A-Z]{3}");
                newUsers.add(new UserProfile(
                        UUID.randomUUID(),
                        name,
                        initials,
                        "https://avatar.worldpinball.com/" + i + ".png",
                        "FR",
                        true,
                        now.minus(i * 10L, ChronoUnit.DAYS)
                ));
            }
            userProfileRepository.saveAll(newUsers);
            users = userProfileRepository.findAll();
        }

        // 5. Machines & Devices (20 machines)
        List<Machine> machines = new ArrayList<>();
        List<Device> devices = new ArrayList<>();
        List<MachineDevice> machineDevices = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            IpLicense ip = licenses.get((i - 1) % licenses.size());
            String serial = String.format("WP-%05d", i);
            String displayName = ip.getName() + " Pro #" + i;

            Machine machine = new Machine(
                    ip.getIpId(),
                    serial,
                    displayName,
                    now.minus(i, ChronoUnit.DAYS),
                    UUID.randomUUID(),
                    faker.internet().slug(),
                    UUID.randomUUID()
            );
            if ((i - 1) % 5 == 0) {
                machine.setStatus(MachineStatus.MAINTENANCE, now);
            } else {
                machine.completeEnrollment(now);
            }
            machines.add(machine);

            Device device = Device.createActive(
                    String.format("DEVICE-%05d", i),
                    developmentPem("PUBLIC KEY", "device-key-" + i),
                    "ecdsa-p256",
                    now.minus(i, ChronoUnit.DAYS),
                    UUID.randomUUID()
            );
            if (i == 1 || i == 2) {
                device.assignOwner(PHILIPPE_USER_ID);
            }
            devices.add(device);

            machineDevices.add(new MachineDevice(machine.getMachineId(), device.getDeviceId(), now.minus(i, ChronoUnit.DAYS)));
        }
        machineRepository.saveAll(machines);
        deviceRepository.saveAll(devices);
        machineDeviceRepository.saveAll(machineDevices);

        // 6 & 7. CA Certificates, Device Certificates, Revocations, Operator Token & Role Permissions
        seedCertificatesAndPermissions(now, devices);

        // 8. Game Sessions & Scores (60 sessions & leaderboard scores)
        List<GameSession> sessions = new ArrayList<>();
        List<ScoreEntry> scores = new ArrayList<>();

        for (int i = 0; i < 60; i++) {
            Machine machine = machines.get(i % machines.size());
            Device device = devices.get(i % devices.size());
            GameMode mode = modes.get(i % modes.size());
            UserProfile user = (i % 4 == 0) ? philippe : users.get((i % (users.size() - 1)) + 1);

            Instant sessionStart = now.minus(faker.number().numberBetween(1, 60), ChronoUnit.DAYS);
            Instant sessionEnd = sessionStart.plus(faker.number().numberBetween(3, 35), ChronoUnit.MINUTES);

            GameSession session = new GameSession(
                    machine.getMachineId(),
                    device.getDeviceId(),
                    mode.getModeId(),
                    "1.0.0",
                    faker.internet().slug(),
                    sessionStart,
                    UUID.randomUUID()
            );
            session.addPlayer(1, user.getUserId(), user.getInitials(), false);
            session.launch(sessionStart);
            session.endSession(sessionEnd);
            sessions.add(session);

            long points = faker.number().numberBetween(10_000L, 25_000_000L);
            ScoreEntry scoreEntry = new ScoreEntry(
                    session.getSessionId(),
                    1,
                    device.getDeviceId(),
                    machine.getMachineId(),
                    machine.getIpId(),
                    mode.getModeId(),
                    points,
                    sessionEnd,
                    user.getUserId()
            );
            if (i % 12 == 0) {
                scoreEntry.invalidate("Score marked for development review.");
            }
            scores.add(scoreEntry);
        }
        gameSessionRepository.saveAll(sessions);
        List<ScoreEntry> savedScores = scoreEntryRepository.saveAll(scores);

        // Warm up Redis ZSET Leaderboard with seeded scores
        for (ScoreEntry s : savedScores) {
            try {
                realtimeLeaderboardService.addScore(s.getIpId(), s.getModeId(), s.getScoreEntryId(), s.getScore());
            } catch (Exception ex) {
                log.warn("Could not warm up Redis ZSET for seeded score {}: {}", s.getScoreEntryId(), ex.getMessage());
            }
        }

        log.info("Development database seeding completed successfully (20 machines, 13 players, 60 scores).");
    }

    private void seedCertificatesAndPermissions(Instant now, List<Device> devices) {
        if (caCertificateRepository.count() == 0) {
            List<CaCertificate> caCertificates = new ArrayList<>();
            for (int index = 1; index <= 2; index++) {
                String commonName = "WorldPinball Development CA " + index;
                CaCertificate ca = new CaCertificate(
                        index == 1 ? CaType.Root : CaType.Intermediate,
                        "CN=" + commonName,
                        index == 1 ? "CN=" + commonName : "CN=WorldPinball Development CA 1",
                        String.format("CA-%04d", index),
                        sha256("ca-certificate-" + index),
                        developmentPem("CERTIFICATE", "ca-certificate-" + index),
                        now.minus(30, ChronoUnit.DAYS),
                        now.plus((index == 1 ? 10 : 5) * 365L, ChronoUnit.DAYS),
                        now,
                        true,
                        index == 2,
                        null
                );
                caCertificates.add(ca);
            }
            caCertificateRepository.saveAll(caCertificates);

            List<DeviceCertificate> deviceCertificates = new ArrayList<>();
            for (int index = 0; index < devices.size(); index++) {
                Device device = devices.get(index);
                CaCertificate ca = caCertificates.get(index % caCertificates.size());
                Instant issuedAt = device.getActivatedAtUtc() != null ? device.getActivatedAtUtc() : device.getCreatedAtUtc();
                DeviceCertificate cert = new DeviceCertificate(
                        device.getDeviceId(),
                        ca.getCaId(),
                        String.format("CERT-%05d", index + 1),
                        sha256("device-certificate-" + (index + 1)),
                        "CN=" + device.getSerialPhysical(),
                        ca.getSubjectDn(),
                        developmentPem("CERTIFICATE", "device-certificate-" + (index + 1)),
                        issuedAt,
                        issuedAt.plus(365, ChronoUnit.DAYS),
                        issuedAt
                );
                deviceCertificates.add(cert);
            }
            deviceCertificateRepository.saveAll(deviceCertificates);

            List<CertificateRevocation> revocations = new ArrayList<>();
            for (int index = 0; index < deviceCertificates.size(); index++) {
                if (index % 8 == 0 && revocations.size() < deviceCertificates.size() / 8) {
                    DeviceCertificate cert = deviceCertificates.get(index);
                    Instant revokedAt = now.minus(revocations.size() + 1L, ChronoUnit.DAYS);
                    String notes = "Development seed revocation";
                    cert.markRevoked(revokedAt, notes);
                    deviceCertificateRepository.save(cert);
                    revocations.add(new CertificateRevocation(
                            cert.getDeviceCertId(),
                            revokedAt,
                            RevocationReason.KeyCompromise,
                            DEVELOPMENT_USER_ID,
                            notes
                    ));
                }
            }
            certificateRevocationRepository.saveAll(revocations);
        }

        if (operatorTokenRepository.findByTokenHash(OperatorToken.hashToken(DEV_OPERATOR_TOKEN)).isEmpty()) {
            OperatorToken devOperatorToken = new OperatorToken(
                    "Local Dev Operator",
                    DEV_OPERATOR_TOKEN,
                    "*",
                    DEVELOPMENT_USER_ID,
                    null,
                    now
            );
            operatorTokenRepository.save(devOperatorToken);
        }

        if (rolePermissionRepository.count() == 0) {
            rolePermissionRepository.saveAll(List.of(
                    new RolePermission("Admin", "MachinesProvision"),
                    new RolePermission("Admin", "MachinesReissueToken"),
                    new RolePermission("Admin", "ShipmentsDispatch"),
                    new RolePermission("Admin", "ShipmentsCancel"),
                    new RolePermission("Admin", "SessionsPlay"),
                    new RolePermission("Admin", "CertificatesIssueSoftware"),
                    new RolePermission("Technician", "MachinesProvision"),
                    new RolePermission("Technician", "MachinesReissueToken"),
                    new RolePermission("Operator", "ShipmentsDispatch"),
                    new RolePermission("Player", "SessionsPlay")
            ));
        } else if (!rolePermissionRepository.existsByRoleAndPermission("Admin", "CertificatesIssueSoftware")) {
            rolePermissionRepository.save(new RolePermission("Admin", "CertificatesIssueSoftware"));
        }
        rolePermissionReloader.reloadFromDatabase();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String developmentPem(String label, String value) {
        String b64 = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        return "-----BEGIN " + label + "-----\n" + b64 + "\n-----END " + label + "-----";
    }
}

package com.worldpinball.leaderboard.application.features.machines.Public;

import com.worldpinball.leaderboard.application.features.machines.Public.enroll.EnrollDeviceCommand;
import com.worldpinball.leaderboard.application.features.machines.Public.enroll.EnrollDeviceHandlerImpl;
import com.worldpinball.leaderboard.application.features.machines.Public.enroll.EnrollDeviceResult;
import com.worldpinball.leaderboard.application.security.ca.CertificateAuthorityService;
import com.worldpinball.leaderboard.application.security.ca.IssuedCertificate;
import com.worldpinball.leaderboard.application.security.secrets.SecretService;
import com.worldpinball.leaderboard.domain.entities.Device;
import com.worldpinball.leaderboard.domain.entities.EnrollmentToken;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.enums.EnrollmentTokenType;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import com.worldpinball.leaderboard.infrastructure.CaCertificateRepository;
import com.worldpinball.leaderboard.infrastructure.DeviceCertificateRepository;
import com.worldpinball.leaderboard.infrastructure.DeviceRepository;
import com.worldpinball.leaderboard.infrastructure.EnrollmentTokenRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollDeviceHandler Tests")
class EnrollDeviceHandlerTest {

    @Mock
    private EnrollmentTokenRepository enrollmentTokenRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private MachineDeviceRepository machineDeviceRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private CaCertificateRepository caCertificateRepository;

    @Mock
    private DeviceCertificateRepository deviceCertificateRepository;

    @Mock
    private CertificateAuthorityService caService;

    private final SecretService secretService = new SecretService();

    private EnrollDeviceHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new EnrollDeviceHandlerImpl(
                enrollmentTokenRepository,
                deviceRepository,
                machineDeviceRepository,
                machineRepository,
                secretService,
                caService,
                caCertificateRepository,
                deviceCertificateRepository
        );
    }

    private record TokenPair(String plainToken, String tokenHash) {}

    private TokenPair generateTokenPair() {
        var result = secretService.generate();
        return new TokenPair(result.plainBase64(), result.hashHex());
    }

    @Test
    @DisplayName("Returns MachineNotFound when machineId is null or empty GUID")
    void handle_whenMachineIdIsEmpty_shouldReturnMachineNotFound() {
        EnrollDeviceCommand command = new EnrollDeviceCommand(
                new UUID(0L, 0L),
                "some-plain-token",
                "PUBLIC_KEY",
                "DEV-12345"
        );

        EnrollDeviceResult result = sut.handle(command);

        assertThat(result).isInstanceOf(EnrollDeviceResult.MachineNotFound.class);
        EnrollDeviceResult.MachineNotFound res = (EnrollDeviceResult.MachineNotFound) result;
        assertThat(res.machineId()).isEqualTo(new UUID(0L, 0L));
    }

    @Test
    @DisplayName("Returns MachineNotFound when machineId in command does not match token machineId")
    void handle_whenMachineIdMismatch_shouldReturnMachineNotFound() {
        TokenPair pair = generateTokenPair();
        UUID tokenMachineId = UUID.randomUUID();
        UUID commandMachineId = UUID.randomUUID();
        Instant now = Instant.now();

        EnrollmentToken token = new EnrollmentToken(
                pair.tokenHash(),
                EnrollmentTokenType.DeviceEnroll,
                "admin-1",
                now,
                now.plusSeconds(3600),
                tokenMachineId
        );

        when(enrollmentTokenRepository.findByTokenHash(pair.tokenHash())).thenReturn(Optional.of(token));

        EnrollDeviceCommand command = new EnrollDeviceCommand(
                commandMachineId,
                pair.plainToken(),
                "PUBLIC_KEY",
                "DEV-12345"
        );

        EnrollDeviceResult result = sut.handle(command);

        assertThat(result).isInstanceOf(EnrollDeviceResult.MachineNotFound.class);
        EnrollDeviceResult.MachineNotFound res = (EnrollDeviceResult.MachineNotFound) result;
        assertThat(res.machineId()).isEqualTo(commandMachineId);
    }

    @Test
    @DisplayName("Returns InvalidToken when token format is invalid")
    void handle_whenTokenFormatIsInvalid_shouldReturnInvalidToken() {
        EnrollDeviceCommand command = new EnrollDeviceCommand(
                UUID.randomUUID(),
                "not-a-valid-base64-token",
                "PUBLIC_KEY",
                "DEV-12345"
        );

        EnrollDeviceResult result = sut.handle(command);

        assertThat(result).isInstanceOf(EnrollDeviceResult.InvalidToken.class);
    }

    @Test
    @DisplayName("Returns InvalidToken when token hash is not in repository")
    void handle_whenTokenDoesNotExistInRepository_shouldReturnInvalidToken() {
        TokenPair pair = generateTokenPair();
        when(enrollmentTokenRepository.findByTokenHash(pair.tokenHash())).thenReturn(Optional.empty());

        EnrollDeviceCommand command = new EnrollDeviceCommand(
                UUID.randomUUID(),
                pair.plainToken(),
                "PUBLIC_KEY",
                "DEV-12345"
        );

        EnrollDeviceResult result = sut.handle(command);

        assertThat(result).isInstanceOf(EnrollDeviceResult.InvalidToken.class);
    }

    @Test
    @DisplayName("Returns DeviceAlreadyEnrolled when device serial is already in repository")
    void handle_whenDeviceSerialAlreadyExistsInRepository_shouldReturnDeviceAlreadyEnrolled() {
        TokenPair pair = generateTokenPair();
        UUID machineId = UUID.randomUUID();
        Instant now = Instant.now();

        EnrollmentToken token = new EnrollmentToken(
                pair.tokenHash(),
                EnrollmentTokenType.DeviceEnroll,
                "admin-1",
                now,
                now.plusSeconds(3600),
                machineId
        );

        when(enrollmentTokenRepository.findByTokenHash(pair.tokenHash())).thenReturn(Optional.of(token));
        when(deviceRepository.existsBySerialPhysical("DEV-12345")).thenReturn(true);

        EnrollDeviceCommand command = new EnrollDeviceCommand(
                machineId,
                pair.plainToken(),
                "PUBLIC_KEY",
                "DEV-12345"
        );

        EnrollDeviceResult result = sut.handle(command);

        assertThat(result).isInstanceOf(EnrollDeviceResult.DeviceAlreadyEnrolled.class);
    }

    @Test
    @DisplayName("Successfully enrolls device, consumes token, updates machine status and issues certificate")
    void handle_whenTokenAndMachineAreValid_shouldReturnSuccess() {
        TokenPair pair = generateTokenPair();
        Instant now = Instant.now();

        Machine machine = new Machine(
                UUID.randomUUID(),
                "WP-TEST-001",
                "Test Machine",
                now,
                UUID.randomUUID(),
                "test-nonce",
                null
        );
        UUID machineId = machine.getMachineId();

        EnrollmentToken token = new EnrollmentToken(
                pair.tokenHash(),
                EnrollmentTokenType.DeviceEnroll,
                "admin-1",
                now,
                now.plusSeconds(3600),
                machineId
        );

        when(enrollmentTokenRepository.findByTokenHash(pair.tokenHash())).thenReturn(Optional.of(token));
        when(deviceRepository.existsBySerialPhysical("DEV-12345")).thenReturn(false);
        when(machineRepository.findById(machineId)).thenReturn(Optional.of(machine));

        IssuedCertificate cert = new IssuedCertificate(
                "-----BEGIN CERTIFICATE-----\nTEST_CERT\n-----END CERTIFICATE-----",
                "cert-serial-123",
                machineId,
                "thumbprint-123",
                "CN=DEV-12345",
                "CN=WorldPinball Intermediate CA",
                now,
                now.plusSeconds(31536000)
        );
        when(caService.issueDeviceCertificate(machineId, "DEV-12345", "PUBLIC_KEY")).thenReturn(cert);

        EnrollDeviceCommand command = new EnrollDeviceCommand(
                machineId,
                pair.plainToken(),
                "PUBLIC_KEY",
                "DEV-12345"
        );

        EnrollDeviceResult result = sut.handle(command);

        assertThat(result).isInstanceOf(EnrollDeviceResult.Success.class);
        EnrollDeviceResult.Success success = (EnrollDeviceResult.Success) result;
        assertThat(success.machineId()).isEqualTo(machineId);
        assertThat(success.certPem()).isEqualTo(cert.certPem());
        assertThat(success.certSerialNumber()).isEqualTo("cert-serial-123");

        assertThat(token.getUsesCount()).isEqualTo(1);
        assertThat(machine.getStatus()).isEqualTo(MachineStatus.ACTIVE);

        verify(deviceRepository).save(any(Device.class));
        verify(machineDeviceRepository).save(any());
        verify(enrollmentTokenRepository).save(token);
        verify(machineRepository).save(machine);
    }

    @Test
    @DisplayName("Returns InvalidToken when enrollment token is expired")
    void handle_whenTokenIsExpired_shouldReturnInvalidToken() {
        TokenPair pair = generateTokenPair();
        UUID machineId = UUID.randomUUID();
        Instant now = Instant.now();

        EnrollmentToken token = new EnrollmentToken(
                pair.tokenHash(),
                EnrollmentTokenType.DeviceEnroll,
                "admin-1",
                now.minusSeconds(7200),
                now.minusSeconds(3600),
                machineId
        );

        when(enrollmentTokenRepository.findByTokenHash(pair.tokenHash())).thenReturn(Optional.of(token));
        when(deviceRepository.existsBySerialPhysical("DEV-12345")).thenReturn(false);

        EnrollDeviceCommand command = new EnrollDeviceCommand(
                machineId,
                pair.plainToken(),
                "PUBLIC_KEY",
                "DEV-12345"
        );

        EnrollDeviceResult result = sut.handle(command);

        assertThat(result).isInstanceOf(EnrollDeviceResult.InvalidToken.class);
        assertThat(token.getUsesCount()).isEqualTo(0);
        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Returns MachineNotFound when machine does not exist in repository")
    void handle_whenMachineDoesNotExistInRepository_shouldReturnMachineNotFound() {
        TokenPair pair = generateTokenPair();
        UUID machineId = UUID.randomUUID();
        Instant now = Instant.now();

        EnrollmentToken token = new EnrollmentToken(
                pair.tokenHash(),
                EnrollmentTokenType.DeviceEnroll,
                "admin-1",
                now,
                now.plusSeconds(3600),
                machineId
        );

        when(enrollmentTokenRepository.findByTokenHash(pair.tokenHash())).thenReturn(Optional.of(token));
        when(deviceRepository.existsBySerialPhysical("DEV-12345")).thenReturn(false);
        when(machineRepository.findById(machineId)).thenReturn(Optional.empty());

        EnrollDeviceCommand command = new EnrollDeviceCommand(
                machineId,
                pair.plainToken(),
                "PUBLIC_KEY",
                "DEV-12345"
        );

        EnrollDeviceResult result = sut.handle(command);

        assertThat(result).isInstanceOf(EnrollDeviceResult.MachineNotFound.class);
        EnrollDeviceResult.MachineNotFound notFound = (EnrollDeviceResult.MachineNotFound) result;
        assertThat(notFound.machineId()).isEqualTo(machineId);
    }

    @Test
    @DisplayName("Returns InvalidToken when machine is already active and cannot complete enrollment")
    void handle_whenMachineCannotCompleteEnrollment_shouldReturnInvalidToken() {
        TokenPair pair = generateTokenPair();
        Instant now = Instant.now();

        Machine machine = new Machine(
                UUID.randomUUID(),
                "WP-TEST-002",
                "Test Machine",
                now,
                UUID.randomUUID(),
                "test-nonce",
                null
        );
        machine.completeEnrollment(now);
        UUID machineId = machine.getMachineId();

        EnrollmentToken token = new EnrollmentToken(
                pair.tokenHash(),
                EnrollmentTokenType.DeviceEnroll,
                "admin-1",
                now,
                now.plusSeconds(3600),
                machineId
        );

        when(enrollmentTokenRepository.findByTokenHash(pair.tokenHash())).thenReturn(Optional.of(token));
        when(deviceRepository.existsBySerialPhysical("DEV-12345")).thenReturn(false);
        when(machineRepository.findById(machineId)).thenReturn(Optional.of(machine));

        EnrollDeviceCommand command = new EnrollDeviceCommand(
                machineId,
                pair.plainToken(),
                "PUBLIC_KEY",
                "DEV-12345"
        );

        EnrollDeviceResult result = sut.handle(command);

        assertThat(result).isInstanceOf(EnrollDeviceResult.InvalidToken.class);
        verify(deviceRepository, never()).save(any());
    }
}

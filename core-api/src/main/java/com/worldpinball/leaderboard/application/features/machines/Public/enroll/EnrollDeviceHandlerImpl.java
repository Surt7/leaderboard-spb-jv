package com.worldpinball.leaderboard.application.features.machines.Public.enroll;

import com.worldpinball.leaderboard.application.security.ca.CertificateAuthorityService;
import com.worldpinball.leaderboard.application.security.ca.IssuedCertificate;
import com.worldpinball.leaderboard.application.security.secrets.SecretService;
import com.worldpinball.leaderboard.domain.entities.CaCertificate;
import com.worldpinball.leaderboard.domain.entities.Device;
import com.worldpinball.leaderboard.domain.entities.DeviceCertificate;
import com.worldpinball.leaderboard.domain.entities.EnrollmentToken;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.entities.MachineDevice;
import com.worldpinball.leaderboard.domain.enums.CaType;
import com.worldpinball.leaderboard.infrastructure.CaCertificateRepository;
import com.worldpinball.leaderboard.infrastructure.DeviceCertificateRepository;
import com.worldpinball.leaderboard.infrastructure.DeviceRepository;
import com.worldpinball.leaderboard.infrastructure.EnrollmentTokenRepository;
import com.worldpinball.leaderboard.infrastructure.MachineDeviceRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class EnrollDeviceHandlerImpl implements EnrollDeviceHandler {

    private final EnrollmentTokenRepository enrollmentTokenRepository;
    private final DeviceRepository deviceRepository;
    private final MachineDeviceRepository machineDeviceRepository;
    private final MachineRepository machineRepository;
    private final SecretService secretService;
    private final CertificateAuthorityService caService;
    private final CaCertificateRepository caCertificateRepository;
    private final DeviceCertificateRepository deviceCertificateRepository;

    public EnrollDeviceHandlerImpl(
            EnrollmentTokenRepository enrollmentTokenRepository,
            DeviceRepository deviceRepository,
            MachineDeviceRepository machineDeviceRepository,
            MachineRepository machineRepository,
            SecretService secretService,
            CertificateAuthorityService caService
    ) {
        this(enrollmentTokenRepository, deviceRepository, machineDeviceRepository,
                machineRepository, secretService, caService, null, null);
    }

    @Autowired
    public EnrollDeviceHandlerImpl(
            EnrollmentTokenRepository enrollmentTokenRepository,
            DeviceRepository deviceRepository,
            MachineDeviceRepository machineDeviceRepository,
            MachineRepository machineRepository,
            SecretService secretService,
            CertificateAuthorityService caService,
            CaCertificateRepository caCertificateRepository,
            DeviceCertificateRepository deviceCertificateRepository
    ) {
        this.enrollmentTokenRepository = enrollmentTokenRepository;
        this.deviceRepository = deviceRepository;
        this.machineDeviceRepository = machineDeviceRepository;
        this.machineRepository = machineRepository;
        this.secretService = secretService;
        this.caService = caService;
        this.caCertificateRepository = caCertificateRepository;
        this.deviceCertificateRepository = deviceCertificateRepository;
    }

    @Override
    @Transactional
    public EnrollDeviceResult handle(EnrollDeviceCommand cmd) {
        Instant now = Instant.now();

        if (cmd.machineId() == null || cmd.machineId().equals(new UUID(0L, 0L))) {
            log.warn("Device enrollment rejected for serial {}: MachineId is empty.", cmd.deviceSerialNumber());
            return new EnrollDeviceResult.MachineNotFound(cmd.machineId());
        }

        if (cmd.plainEnrollmentToken() == null || cmd.plainEnrollmentToken().isBlank()) {
            return new EnrollDeviceResult.InvalidToken("PlainEnrollmentToken cannot be null or empty");
        }

        Optional<String> tokenHashOpt = secretService.tryHash(cmd.plainEnrollmentToken());
        if (tokenHashOpt.isEmpty()) {
            log.warn("Device enrollment rejected for serial {}: Invalid token format.", cmd.deviceSerialNumber());
            return new EnrollDeviceResult.InvalidToken("Invalid token format");
        }

        Optional<EnrollmentToken> tokenOpt = enrollmentTokenRepository.findByTokenHash(tokenHashOpt.get());
        if (tokenOpt.isEmpty()) {
            log.warn("Device enrollment rejected for serial {}: Token not found.", cmd.deviceSerialNumber());
            return new EnrollDeviceResult.InvalidToken("Token not found");
        }

        EnrollmentToken token = tokenOpt.get();
        if (!cmd.machineId().equals(token.getMachineId())) {
            log.warn("Device enrollment rejected: MachineId mismatch. Token authorized for {} but requested {}.",
                    token.getMachineId(), cmd.machineId());
            return new EnrollDeviceResult.MachineNotFound(cmd.machineId());
        }

        if (deviceRepository.existsBySerialPhysical(cmd.deviceSerialNumber().trim())) {
            log.warn("Device enrollment rejected for serial {}: Device is already enrolled.", cmd.deviceSerialNumber());
            return new EnrollDeviceResult.DeviceAlreadyEnrolled(cmd.deviceSerialNumber());
        }

        Device device = Device.enroll(
                cmd.deviceSerialNumber(),
                cmd.publicKeyPem(),
                now
        );

        MachineDevice machineDevice = new MachineDevice(
                cmd.machineId(),
                device.getDeviceId(),
                now
        );

        try {
            token.consume(device.getDeviceId(), now);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            log.warn("Device enrollment rejected for serial {}: Token consumption failed ({}).",
                    cmd.deviceSerialNumber(), ex.getMessage());
            return new EnrollDeviceResult.InvalidToken(ex.getMessage());
        }

        Optional<Machine> machineOpt = machineRepository.findById(cmd.machineId());
        if (machineOpt.isEmpty()) {
            log.warn("Device enrollment rejected: Machine {} not found.", cmd.machineId());
            return new EnrollDeviceResult.MachineNotFound(cmd.machineId());
        }

        Machine machine = machineOpt.get();
        try {
            machine.completeEnrollment(now);
        } catch (IllegalStateException ex) {
            log.warn("Machine {} cannot complete enrollment: {}", cmd.machineId(), ex.getMessage());
            return new EnrollDeviceResult.InvalidToken(ex.getMessage());
        }

        deviceRepository.save(device);
        machineDeviceRepository.save(machineDevice);
        enrollmentTokenRepository.save(token);
        machineRepository.save(machine);

        IssuedCertificate issuedCert = caService.issueDeviceCertificate(
                cmd.machineId(),
                cmd.deviceSerialNumber(),
                cmd.publicKeyPem()
        );

        if (deviceCertificateRepository != null) {
            UUID caId = UUID.randomUUID();
            if (caCertificateRepository != null) {
                caId = caCertificateRepository.findFirstByCaTypeAndActiveTrue(CaType.Intermediate)
                        .or(() -> caCertificateRepository.findFirstByActiveTrueOrderByCreatedAtDesc())
                        .map(CaCertificate::getCaId)
                        .orElse(caId);
            }

            DeviceCertificate deviceCert = new DeviceCertificate(
                    device.getDeviceId(),
                    caId,
                    issuedCert.serialNumber(),
                    issuedCert.thumbprintSha256(),
                    issuedCert.subjectDn(),
                    issuedCert.issuerDn(),
                    issuedCert.certPem(),
                    issuedCert.notBeforeUtc(),
                    issuedCert.notAfterUtc(),
                    now
            );
            deviceCertificateRepository.save(deviceCert);
        }

        log.info("Device {} (serial: {}) enrolled and attached to machine {} with CertSerial {}.",
                device.getDeviceId(), cmd.deviceSerialNumber(), cmd.machineId(), issuedCert.serialNumber());

        return new EnrollDeviceResult.Success(
                device.getDeviceId(),
                cmd.machineId(),
                issuedCert.certPem(),
                issuedCert.serialNumber()
        );
    }
}

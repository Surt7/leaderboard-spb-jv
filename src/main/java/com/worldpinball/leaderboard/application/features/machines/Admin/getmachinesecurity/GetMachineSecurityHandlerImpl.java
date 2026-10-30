package com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity;

import com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity.dtos.MachineSecurityDto;
import com.worldpinball.leaderboard.application.features.machines.Admin.getmachinesecurity.queries.GetMachineSecurityQuery;
import com.worldpinball.leaderboard.domain.entities.Device;
import com.worldpinball.leaderboard.domain.entities.DeviceCertificate;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.entities.MachineDevice;
import com.worldpinball.leaderboard.domain.enums.DeviceCertificateStatus;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import com.worldpinball.leaderboard.infrastructure.DeviceCertificateRepository;
import com.worldpinball.leaderboard.infrastructure.DeviceRepository;
import com.worldpinball.leaderboard.infrastructure.MachineDeviceRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetMachineSecurityHandlerImpl implements GetMachineSecurityHandler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC);

    private final MachineRepository machineRepository;
    private final MachineDeviceRepository machineDeviceRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceCertificateRepository deviceCertificateRepository;

    @Override
    @Transactional(readOnly = true)
    public GetMachineSecurityResult handle(GetMachineSecurityQuery query) {
        log.info("Fetching security and TPM certificate status for machine {}", query.machineId());

        Optional<Machine> machineOpt = machineRepository.findById(query.machineId());
        if (machineOpt.isEmpty()) {
            log.warn("Machine with id {} not found.", query.machineId());
            return new GetMachineSecurityResult.MachineNotFound(query.machineId());
        }

        Machine machine = machineOpt.get();
        int machineStatusInt = mapMachineStatus(machine.getStatus());

        Optional<MachineDevice> activeDeviceLink = machineDeviceRepository.findFirstByMachineIdAndActiveTrue(query.machineId());
        if (activeDeviceLink.isEmpty()) {
            return new GetMachineSecurityResult.Success(new MachineSecurityDto(
                    machine.getMachineId(),
                    machine.getWorldPinballSerialNumber(),
                    machine.getDisplayName(),
                    machineStatusInt,
                    false,
                    null,
                    null,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    "Aucun périphérique physique (TPM) n'est actuellement rattaché à ce flipper."
            ));
        }

        Optional<Device> deviceOpt = deviceRepository.findById(activeDeviceLink.get().getDeviceId());
        if (deviceOpt.isEmpty()) {
            return new GetMachineSecurityResult.Success(new MachineSecurityDto(
                    machine.getMachineId(),
                    machine.getWorldPinballSerialNumber(),
                    machine.getDisplayName(),
                    machineStatusInt,
                    false,
                    null,
                    null,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    "Aucun périphérique physique (TPM) n'est actuellement rattaché à ce flipper."
            ));
        }

        Device device = deviceOpt.get();
        Optional<DeviceCertificate> activeCertOpt = deviceCertificateRepository.findFirstByDeviceIdOrderByIssuedAtUtcDesc(device.getDeviceId());
        if (activeCertOpt.isEmpty()) {
            return new GetMachineSecurityResult.Success(new MachineSecurityDto(
                    machine.getMachineId(),
                    machine.getWorldPinballSerialNumber(),
                    machine.getDisplayName(),
                    machineStatusInt,
                    true,
                    device.getDeviceId(),
                    device.getSerialPhysical(),
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    "Périphérique TPM détecté mais aucun certificat n'a été émis. Enrôlement requis."
            ));
        }

        DeviceCertificate activeCert = activeCertOpt.get();
        Instant now = Instant.now();
        boolean isExpired = now.isAfter(activeCert.getNotAfterUtc());
        boolean isRevoked = activeCert.getStatus() == DeviceCertificateStatus.Revoked;
        boolean isValid = activeCert.getStatus() == DeviceCertificateStatus.Active
                && !isExpired
                && !now.isBefore(activeCert.getNotBeforeUtc());

        String explanation;
        if (isValid) {
            explanation = "Certificat matériel TPM valide et vérifié. Liaison mTLS opérationnelle.";
        } else if (isRevoked) {
            String revokedDate = activeCert.getRevokedAtUtc() != null ? DATE_FORMATTER.format(activeCert.getRevokedAtUtc()) : "";
            explanation = String.format("Certificat révoqué le %s : %s", revokedDate, activeCert.getRevokeReason());
        } else if (isExpired) {
            explanation = String.format("Certificat matériel expiré depuis le %s.", DATE_FORMATTER.format(activeCert.getNotAfterUtc()));
        } else {
            explanation = "Certificat non actif.";
        }

        return new GetMachineSecurityResult.Success(new MachineSecurityDto(
                machine.getMachineId(),
                machine.getWorldPinballSerialNumber(),
                machine.getDisplayName(),
                machineStatusInt,
                true,
                device.getDeviceId(),
                device.getSerialPhysical(),
                true,
                activeCert.getCertSerialNumber(),
                activeCert.getCertThumbprintSha256(),
                activeCert.getSubjectDn(),
                activeCert.getIssuerDn(),
                activeCert.getNotBeforeUtc(),
                activeCert.getNotAfterUtc(),
                activeCert.getStatus().getValue(),
                isExpired,
                isValid,
                explanation
        ));
    }

    public static int mapMachineStatus(MachineStatus status) {
        if (status == null) return 0;
        return switch (status) {
            case ACTIVE -> 1;
            case REVOKED -> 2;
            case INACTIVE -> 3;
            case MAINTENANCE -> 4;
            case PENDING_ENROLLMENT -> 5;
        };
    }
}

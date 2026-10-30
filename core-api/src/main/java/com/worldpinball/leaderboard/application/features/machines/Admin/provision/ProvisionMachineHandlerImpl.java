package com.worldpinball.leaderboard.application.features.machines.Admin.provision;

import com.worldpinball.leaderboard.application.features.machines.Admin.provision.commands.ProvisionMachineCommand;
import com.worldpinball.leaderboard.application.security.secrets.SecretGenerationResult;
import com.worldpinball.leaderboard.application.security.secrets.SecretService;
import com.worldpinball.leaderboard.domain.entities.EnrollmentToken;
import com.worldpinball.leaderboard.domain.entities.IpLicense;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.enums.EnrollmentTokenType;
import com.worldpinball.leaderboard.infrastructure.EnrollmentTokenRepository;
import com.worldpinball.leaderboard.infrastructure.IpLicenseRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProvisionMachineHandlerImpl implements ProvisionMachineHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProvisionMachineHandlerImpl.class);

    private final IpLicenseRepository ipLicenseRepository;
    private final MachineRepository machineRepository;
    private final EnrollmentTokenRepository enrollmentTokenRepository;
    private final SecretService secretService;
    private final SecureRandom secureRandom = new SecureRandom();

    public ProvisionMachineHandlerImpl(
            IpLicenseRepository ipLicenseRepository,
            MachineRepository machineRepository,
            EnrollmentTokenRepository enrollmentTokenRepository,
            SecretService secretService
    ) {
        this.ipLicenseRepository = ipLicenseRepository;
        this.machineRepository = machineRepository;
        this.enrollmentTokenRepository = enrollmentTokenRepository;
        this.secretService = secretService;
    }

    @Override
    @Transactional
    public ProvisionMachineResult handle(ProvisionMachineCommand cmd) {
        Optional<IpLicense> ipLicenseOpt = ipLicenseRepository.findBySlugIgnoreCaseAndActiveTrue(cmd.ipSlug());
        if (ipLicenseOpt.isEmpty()) {
            logger.warn("[2001:MachineProvisioningUnknownIp] Machine provisioning rejected: IP {} was not found.", cmd.ipSlug());
            return new ProvisionMachineResult.UnknownIp(cmd.ipSlug());
        }

        IpLicense ipLicense = ipLicenseOpt.get();

        if (machineRepository.existsByIdempotencyKey(cmd.idempotencyKey())) {
            logger.warn("[2003:MachineProvisioningSameIdempotencyKey] Same Idempotency key {} already exists.", cmd.idempotencyKey());
            return new ProvisionMachineResult.SameIdempotencyKey(cmd.idempotencyKey());
        }

        if (machineRepository.existsByWorldPinballSerialNumber(cmd.serialNumber())) {
            logger.warn("[2002:MachineProvisioningDuplicateSerial] Machine provisioning rejected: serial number {} already exists.", cmd.serialNumber());
            return new ProvisionMachineResult.DuplicateSerial(cmd.serialNumber());
        }

        Instant nowUtc = Instant.now();
        byte[] nonceBytes = new byte[32];
        secureRandom.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);

        Machine machine = new Machine(
                ipLicense.getIpId(),
                cmd.serialNumber(),
                cmd.displayName(),
                nowUtc,
                cmd.idempotencyKey(),
                nonce,
                UUID.randomUUID()
        );
        machineRepository.saveAndFlush(machine);

        SecretGenerationResult secret = secretService.generate();
        EnrollmentToken enrollmentToken = new EnrollmentToken(
                secret.hashHex(),
                EnrollmentTokenType.DeviceEnroll,
                cmd.issuedByUserId(),
                nowUtc,
                nowUtc.plus(24, ChronoUnit.HOURS),
                machine.getMachineId(),
                null,
                1,
                null,
                24
        );
        enrollmentTokenRepository.save(enrollmentToken);

        logger.info("[2000:MachineProvisioned] Machine {} provisioned for IP {} ({}) by {}.",
                machine.getMachineId(), machine.getIpId(), cmd.ipSlug(), cmd.issuedByUserId());

        return new ProvisionMachineResult.Success(machine.getMachineId(), secret.plainBase64());
    }
}

package com.worldpinball.leaderboard.application.features.machines.Admin.reissuetoken;

import com.worldpinball.leaderboard.application.features.machines.Admin.reissuetoken.commands.ReissueEnrollmentTokenCommand;
import com.worldpinball.leaderboard.application.security.secrets.SecretGenerationResult;
import com.worldpinball.leaderboard.application.security.secrets.SecretService;
import com.worldpinball.leaderboard.domain.entities.EnrollmentToken;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.enums.EnrollmentTokenType;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import com.worldpinball.leaderboard.infrastructure.EnrollmentTokenRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class ReissueEnrollmentTokenHandlerImpl implements ReissueEnrollmentTokenHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReissueEnrollmentTokenHandlerImpl.class);

    private final MachineRepository machineRepository;
    private final EnrollmentTokenRepository enrollmentTokenRepository;
    private final SecretService secretService;

    public ReissueEnrollmentTokenHandlerImpl(
            MachineRepository machineRepository,
            EnrollmentTokenRepository enrollmentTokenRepository,
            SecretService secretService
    ) {
        this.machineRepository = machineRepository;
        this.enrollmentTokenRepository = enrollmentTokenRepository;
        this.secretService = secretService;
    }

    @Override
    @Transactional
    public ReissueEnrollmentTokenResult handle(ReissueEnrollmentTokenCommand cmd) {
        Optional<Machine> machineOpt = machineRepository.findById(cmd.machineId());
        if (machineOpt.isEmpty()) {
            logger.warn("[2051:MachineNotFound] Machine {} was not found.", cmd.machineId());
            return new ReissueEnrollmentTokenResult.MachineNotFound(cmd.machineId());
        }

        Machine machine = machineOpt.get();
        if (machine.getStatus() != MachineStatus.PENDING_ENROLLMENT) {
            logger.warn("[2052:MachineStatusInvalid] Machine {} is in status {} (expected PendingEnrollment).",
                    cmd.machineId(), machine.getStatus());
            return new ReissueEnrollmentTokenResult.InvalidMachineStatus(cmd.machineId(), machine.getStatus());
        }

        Instant nowUtc = Instant.now();
        Instant expiresAtUtc = nowUtc.plus(24, ChronoUnit.HOURS);

        SecretGenerationResult secret = secretService.generate();

        EnrollmentToken enrollmentToken = new EnrollmentToken(
                secret.hashHex(),
                EnrollmentTokenType.DeviceEnroll,
                cmd.issuedByUserId(),
                nowUtc,
                expiresAtUtc,
                machine.getMachineId(),
                null,
                1,
                null,
                24
        );
        enrollmentTokenRepository.save(enrollmentToken);

        logger.info("[2050:EnrollmentTokenReissued] Enrollment token reissued for machine {} by user {}.",
                machine.getMachineId(), cmd.issuedByUserId());

        return new ReissueEnrollmentTokenResult.Success(
                machine.getMachineId(),
                secret.plainBase64(),
                expiresAtUtc
        );
    }
}

package com.worldpinball.leaderboard.infrastructure.security.cryptography;

import com.worldpinball.leaderboard.application.security.cryptography.MachineAuthContext;
import com.worldpinball.leaderboard.application.security.cryptography.MachineAuthResult;
import com.worldpinball.leaderboard.domain.entities.Device;
import com.worldpinball.leaderboard.domain.entities.MachineDevice;
import com.worldpinball.leaderboard.domain.enums.DeviceStatus;
import com.worldpinball.leaderboard.infrastructure.DeviceRepository;
import com.worldpinball.leaderboard.infrastructure.MachineDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Authenticates signed requests originating from physical pinball machines.
 * Enforces header format, +/-120s anti-replay timestamp window, active device attachment,
 * non-revoked status, and ECDSA P-256 signature over the canonical request payload.
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.Cryptography.MachineAuthService}.
 */
@Service
public class MachineAuthService {

    private static final Logger log = LoggerFactory.getLogger(MachineAuthService.class);
    private static final long MAX_TIMESTAMP_DRIFT_SECONDS = 120L;

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String INVALID_DEVICE_ID = "Invalid or missing GUID format.";
        public static final String INVALID_TIMESTAMP = "Invalid or missing Unix timestamp.";
        public static final String INVALID_NONCE = "Nonce is missing or too short (minimum 16 characters).";
        public static final String MISSING_SIGNATURE = "Signature header is missing.";
        public static final String INVALID_SIGNATURE = "Cryptographic signature verification failed.";
    }

    private final MachineDeviceRepository machineDeviceRepository;
    private final DeviceRepository deviceRepository;
    private final CanonicalBuilder canonicalBuilder;
    private final SignatureVerifier signatureVerifier;
    private final Clock clock;

    @Autowired
    public MachineAuthService(
            MachineDeviceRepository machineDeviceRepository,
            DeviceRepository deviceRepository,
            CanonicalBuilder canonicalBuilder,
            SignatureVerifier signatureVerifier) {
        this(machineDeviceRepository, deviceRepository, canonicalBuilder, signatureVerifier, Clock.systemUTC());
    }

    public MachineAuthService(
            MachineDeviceRepository machineDeviceRepository,
            DeviceRepository deviceRepository,
            CanonicalBuilder canonicalBuilder,
            SignatureVerifier signatureVerifier,
            Clock clock) {
        this.machineDeviceRepository = machineDeviceRepository;
        this.deviceRepository = deviceRepository;
        this.canonicalBuilder = canonicalBuilder;
        this.signatureVerifier = signatureVerifier;
        this.clock = clock;
    }

    public MachineAuthResult validate(MachineAuthContext context) {
        UUID deviceId;
        try {
            if (context.deviceIdHeader() == null || context.deviceIdHeader().isBlank()) {
                return invalidHeader("X-Device-Id", ErrorMessages.INVALID_DEVICE_ID);
            }
            deviceId = UUID.fromString(context.deviceIdHeader().trim());
        } catch (IllegalArgumentException ex) {
            return invalidHeader("X-Device-Id", ErrorMessages.INVALID_DEVICE_ID);
        }

        long timestamp;
        try {
            if (context.timestampHeader() == null || context.timestampHeader().isBlank()) {
                return invalidHeader("X-Timestamp", ErrorMessages.INVALID_TIMESTAMP);
            }
            timestamp = Long.parseLong(context.timestampHeader().trim());
        } catch (NumberFormatException ex) {
            return invalidHeader("X-Timestamp", ErrorMessages.INVALID_TIMESTAMP);
        }

        if (context.nonceHeader() == null || context.nonceHeader().isBlank() || context.nonceHeader().length() < 16) {
            return invalidHeader("X-Nonce", ErrorMessages.INVALID_NONCE);
        }

        if (context.signatureHeader() == null || context.signatureHeader().isBlank()) {
            return invalidHeader("X-Signature", ErrorMessages.MISSING_SIGNATURE);
        }

        // Anti-replay timestamp window (+/- 120s)
        long nowSeconds = clock.instant().getEpochSecond();
        long diff = Math.abs(nowSeconds - timestamp);
        if (diff > MAX_TIMESTAMP_DRIFT_SECONDS) {
            return new MachineAuthResult.TimestampOutOfWindow(diff);
        }

        Optional<MachineDevice> activeAttachmentOpt = machineDeviceRepository.findByDeviceIdAndActiveTrue(deviceId);
        if (activeAttachmentOpt.isEmpty()) {
            log.warn("[3102:DeviceNotAttached] Machine authentication rejected: Device {} is not actively attached to any machine.", deviceId);
            return new MachineAuthResult.DeviceNotAttached(deviceId);
        }

        MachineDevice attachment = activeAttachmentOpt.get();
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty() || deviceOpt.get().getStatus() != DeviceStatus.ACTIVE) {
            log.warn("[3103:DeviceRevokedOrInactive] Machine authentication rejected: Device {} is missing or not Active.", deviceId);
            return new MachineAuthResult.DeviceNotFoundOrRevoked(deviceId);
        }

        Device device = deviceOpt.get();
        if (device.getPublicKey() == null || device.getPublicKey().isBlank()) {
            log.warn("[3104:DeviceMissingPublicKey] Machine authentication rejected: Device {} has no registered public key.", deviceId);
            return new MachineAuthResult.MissingPublicKey(deviceId);
        }

        byte[] canonicalBytes = canonicalBuilder.build(
                timestamp,
                context.nonceHeader(),
                context.httpMethod(),
                context.requestPath(),
                context.rawBody()
        );

        boolean isValidSignature;
        try {
            isValidSignature = signatureVerifier.verify(
                    device.getPublicKey(),
                    canonicalBytes,
                    context.signatureHeader(),
                    device.getKeyAlgorithm()
            );
        } catch (Exception ex) {
            log.warn("[3105:SignatureVerificationFailed] Signature verification threw an exception for device {}.", deviceId, ex);
            isValidSignature = false;
        }

        if (!isValidSignature) {
            log.warn("[3105:SignatureVerificationFailed] Signature mismatch for device {}.", deviceId);
            return new MachineAuthResult.InvalidSignature(ErrorMessages.INVALID_SIGNATURE);
        }

        log.info("[3106:MachineAuthenticated] Machine {} successfully authenticated using device {}.",
                attachment.getMachineId(), deviceId);

        return new MachineAuthResult.Success(
                attachment.getMachineId(),
                deviceId,
                context.nonceHeader(),
                timestamp
        );
    }

    private MachineAuthResult.InvalidHeader invalidHeader(String headerName, String reason) {
        log.warn("[3100:InvalidHeaderReceived] Machine authentication failed: Header {} is invalid ({}).", headerName, reason);
        return new MachineAuthResult.InvalidHeader(headerName, reason);
    }
}

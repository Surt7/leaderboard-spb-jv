package com.worldpinball.leaderboard.infrastructure.security.cryptography;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldpinball.leaderboard.application.security.cryptography.MachineAuthContext;
import com.worldpinball.leaderboard.application.security.cryptography.MachineAuthResult;
import com.worldpinball.leaderboard.domain.entities.Device;
import com.worldpinball.leaderboard.domain.entities.MachineDevice;
import com.worldpinball.leaderboard.infrastructure.DeviceRepository;
import com.worldpinball.leaderboard.infrastructure.MachineDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Machine Auth, Canonicalization and ECDSA Signature Tests")
class MachineAuthAndSignatureTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-17T12:00:00Z");

    @Mock
    private MachineDeviceRepository machineDeviceRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private CanonicalBuilder canonicalBuilderMock;

    @Mock
    private SignatureVerifier signatureVerifierMock;

    private MachineAuthService sut;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        sut = new MachineAuthService(
                machineDeviceRepository,
                deviceRepository,
                canonicalBuilderMock,
                signatureVerifierMock,
                fixedClock
        );
    }

    @Test
    @DisplayName("CanonicalJson produces deterministic RFC 8785 output across permutations")
    void canonicalJson_sortsKeysDeterministically() {
        ObjectMapper objectMapper = new ObjectMapper();

        String jsonA = "{\"b\": 2, \"a\": 1, \"nested\": {\"z\": 26, \"y\": 25}}";
        String jsonB = "{\"nested\": {\"y\": 25, \"z\": 26}, \"a\": 1, \"b\": 2}";

        String canonicalA = CanonicalJson.canonicalize(jsonA);
        String canonicalB = CanonicalJson.canonicalize(jsonB);

        assertThat(canonicalA).isEqualTo(canonicalB);
        assertThat(canonicalA).isEqualTo("{\"a\":1,\"b\":2,\"nested\":{\"y\":25,\"z\":26}}");
    }

    @Test
    @DisplayName("EcdsaP256SignatureVerifier verifies both ASN.1 DER and IEEE P1363 (64-byte raw) signatures")
    void ecdsaP256SignatureVerifier_verifiesDerAndP1363Signatures() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyGen.generateKeyPair();

        String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";

        CanonicalBuilder canonicalBuilder = new CanonicalBuilder();
        byte[] payload = canonicalBuilder.build(1758110400L, "1234567890123456", "post", "/api/machines/telemetry", "{\"score\":1000}");

        // 1. ASN.1 DER signature
        Signature derSigner = Signature.getInstance("SHA256withECDSA");
        derSigner.initSign(keyPair.getPrivate());
        derSigner.update(payload);
        String derSigBase64 = Base64.getEncoder().encodeToString(derSigner.sign());

        // 2. IEEE P1363 64-byte (r || s) signature
        Signature p1363Signer = Signature.getInstance("SHA256withECDSAinP1363Format");
        p1363Signer.initSign(keyPair.getPrivate());
        p1363Signer.update(payload);
        byte[] p1363Bytes = p1363Signer.sign();
        assertThat(p1363Bytes).hasSize(64);
        String p1363SigBase64 = Base64.getEncoder().encodeToString(p1363Bytes);

        EcdsaP256SignatureVerifier verifier = new EcdsaP256SignatureVerifier();

        assertThat(verifier.verify(publicKeyPem, payload, derSigBase64, "ecdsa-p256-sha256")).isTrue();
        assertThat(verifier.verify(publicKeyPem, payload, p1363SigBase64, "ecdsa-p256-sha256")).isTrue();
        assertThat(verifier.verify(publicKeyPem, "tampered".getBytes(StandardCharsets.UTF_8), derSigBase64, "ecdsa-p256-sha256")).isFalse();
    }

    @Test
    @DisplayName("validate with valid signature returns Success")
    void validate_withValidSignature_returnsSuccess() {
        UUID machineId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        long timestamp = FIXED_NOW.getEpochSecond();
        String nonce = "1234567890123456";

        Device device = Device.createActive("DEV-001", "pk", "ecdsa-p256-sha256", FIXED_NOW, deviceId);
        MachineDevice machineDevice = new MachineDevice(machineId, deviceId, FIXED_NOW);

        when(machineDeviceRepository.findByDeviceIdAndActiveTrue(deviceId)).thenReturn(Optional.of(machineDevice));
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(canonicalBuilderMock.build(timestamp, nonce, "POST", "/api/machines/telemetry", "{}"))
                .thenReturn(new byte[]{1, 2, 3});
        when(signatureVerifierMock.verify(eq("pk"), any(), eq("valid-signature-base64"), eq("ecdsa-p256-sha256")))
                .thenReturn(true);

        MachineAuthContext context = new MachineAuthContext(
                deviceId.toString(),
                Long.toString(timestamp),
                nonce,
                "valid-signature-base64",
                "POST",
                "/api/machines/telemetry",
                "{}"
        );

        MachineAuthResult result = sut.validate(context);

        assertThat(result).isInstanceOf(MachineAuthResult.Success.class);
        MachineAuthResult.Success success = (MachineAuthResult.Success) result;
        assertThat(success.machineId()).isEqualTo(machineId);
        assertThat(success.deviceId()).isEqualTo(deviceId);
    }

    @Test
    @DisplayName("validate with invalid signature returns InvalidSignature")
    void validate_withInvalidSignature_returnsInvalidSignature() {
        UUID machineId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        long timestamp = FIXED_NOW.getEpochSecond();

        Device device = Device.createActive("DEV-001", "pk", "ecdsa-p256-sha256", FIXED_NOW, deviceId);
        MachineDevice machineDevice = new MachineDevice(machineId, deviceId, FIXED_NOW);

        when(machineDeviceRepository.findByDeviceIdAndActiveTrue(deviceId)).thenReturn(Optional.of(machineDevice));
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(canonicalBuilderMock.build(any(Long.class), any(), any(), any(), any()))
                .thenReturn(new byte[]{1});
        when(signatureVerifierMock.verify(any(), any(), any(), any())).thenReturn(false);

        MachineAuthContext context = new MachineAuthContext(
                deviceId.toString(),
                Long.toString(timestamp),
                "1234567890123456",
                "wrong-signature",
                "POST",
                "/api/endpoint",
                "{}"
        );

        MachineAuthResult result = sut.validate(context);

        assertThat(result).isInstanceOf(MachineAuthResult.InvalidSignature.class);
    }

    @Test
    @DisplayName("validate when DeviceId is invalid returns InvalidHeader")
    void validate_whenDeviceIdInvalid_returnsInvalidHeader() {
        MachineAuthContext context = new MachineAuthContext(
                "wrong-id",
                Long.toString(FIXED_NOW.getEpochSecond()),
                "1234567890123456",
                "fake-signature",
                "POST",
                "/api/endpoint",
                "{}"
        );

        MachineAuthResult result = sut.validate(context);

        assertThat(result).isInstanceOf(MachineAuthResult.InvalidHeader.class);
    }

    @Test
    @DisplayName("validate when timestamp is outside 120s window returns TimestampOutOfWindow")
    void validate_whenTimestampTooOld_returnsTimestampOutOfWindow() {
        long oldTimestamp = FIXED_NOW.plusSeconds(150).getEpochSecond();

        MachineAuthContext context = new MachineAuthContext(
                UUID.randomUUID().toString(),
                Long.toString(oldTimestamp),
                "1234567890123456",
                "fake-signature",
                "POST",
                "/api/endpoint",
                "{}"
        );

        MachineAuthResult result = sut.validate(context);

        assertThat(result).isInstanceOf(MachineAuthResult.TimestampOutOfWindow.class);
    }

    @Test
    @DisplayName("validate when device is revoked returns DeviceNotFoundOrRevoked")
    void validate_whenDeviceIsRevoked_returnsDeviceNotFoundOrRevoked() {
        UUID machineId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        long timestamp = FIXED_NOW.getEpochSecond();

        Device device = Device.createActive("DEV-001", "pk", "ecdsa-p256-sha256", FIXED_NOW, deviceId);
        device.revoke(FIXED_NOW, "Compromised");
        MachineDevice machineDevice = new MachineDevice(machineId, deviceId, FIXED_NOW);

        when(machineDeviceRepository.findByDeviceIdAndActiveTrue(deviceId)).thenReturn(Optional.of(machineDevice));
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        MachineAuthContext context = new MachineAuthContext(
                deviceId.toString(),
                Long.toString(timestamp),
                "1234567890123456",
                "fake-signature",
                "POST",
                "/api/endpoint",
                "{}"
        );

        MachineAuthResult result = sut.validate(context);

        assertThat(result).isInstanceOf(MachineAuthResult.DeviceNotFoundOrRevoked.class);
    }

    @Test
    @DisplayName("validate when device is detached returns DeviceNotAttached")
    void validate_whenDeviceIsDetached_returnsDeviceNotAttached() {
        UUID deviceId = UUID.randomUUID();
        long timestamp = FIXED_NOW.getEpochSecond();

        when(machineDeviceRepository.findByDeviceIdAndActiveTrue(deviceId)).thenReturn(Optional.empty());

        MachineAuthContext context = new MachineAuthContext(
                deviceId.toString(),
                Long.toString(timestamp),
                "1234567890123456",
                "fake-signature",
                "POST",
                "/api/endpoint",
                "{}"
        );

        MachineAuthResult result = sut.validate(context);

        assertThat(result).isInstanceOf(MachineAuthResult.DeviceNotAttached.class);
    }
}

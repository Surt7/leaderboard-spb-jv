package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Machine Domain Entity Unit Tests")
class MachineTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-09-02T12:00:00Z");

    @Test
    void constructor_whenValidArguments_initializesInPendingEnrollment() {
        UUID ipId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        Machine machine = new Machine(
                ipId,
                "WP-K7M4-9X2P-R8HD",
                "Godzilla — Salle principale",
                REGISTERED_AT,
                idempotencyKey,
                "nonce-abc",
                null
        );

        assertThat(machine.getMachineId()).isNotNull();
        assertThat(machine.getIpId()).isEqualTo(ipId);
        assertThat(machine.getWorldPinballSerialNumber()).isEqualTo("WP-K7M4-9X2P-R8HD");
        assertThat(machine.getDisplayName()).isEqualTo("Godzilla — Salle principale");
        assertThat(machine.getRegisteredAtUtc()).isEqualTo(REGISTERED_AT);
        assertThat(machine.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(machine.getNonce()).isEqualTo("nonce-abc");
        assertThat(machine.getStatus()).isEqualTo(MachineStatus.PENDING_ENROLLMENT);
    }

    @Test
    void constructor_whenIpIdIsNull_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new Machine(
                null, "WP-1", "Godzilla", REGISTERED_AT, UUID.randomUUID(), "nonce", null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage(Machine.ErrorMessages.EMPTY_IP_ID);
    }

    @Test
    void constructor_whenSerialNumberIsBlank_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new Machine(
                UUID.randomUUID(), "   ", "Godzilla", REGISTERED_AT, UUID.randomUUID(), "nonce", null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage(Machine.ErrorMessages.EMPTY_SERIAL_NUMBER);
    }

    @Test
    void constructor_whenDisplayNameIsBlank_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new Machine(
                UUID.randomUUID(), "WP-1", "", REGISTERED_AT, UUID.randomUUID(), "nonce", null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage(Machine.ErrorMessages.EMPTY_DISPLAY_NAME);
    }

    @Test
    void constructor_whenIdempotencyKeyIsNull_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new Machine(
                UUID.randomUUID(), "WP-1", "Godzilla", REGISTERED_AT, null, "nonce", null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage(Machine.ErrorMessages.EMPTY_IDEMPOTENCY_KEY);
    }

    @Test
    void constructor_whenNonceIsBlank_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new Machine(
                UUID.randomUUID(), "WP-1", "Godzilla", REGISTERED_AT, UUID.randomUUID(), "   ", null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage(Machine.ErrorMessages.EMPTY_NONCE);
    }

    @Test
    void activate_whenPendingEnrollment_throwsIllegalStateException() {
        Machine machine = new Machine(
                UUID.randomUUID(), "WP-1", "Godzilla", REGISTERED_AT, UUID.randomUUID(), "nonce", null
        );

        assertThatThrownBy(() -> machine.activate(Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(Machine.ErrorMessages.PENDING_ENROLLMENT_REQUIRES_COMPLETION);
    }

    @Test
    void activate_whenRevoked_throwsIllegalStateException() {
        Machine machine = new Machine(
                UUID.randomUUID(), "WP-1", "Godzilla", REGISTERED_AT, UUID.randomUUID(), "nonce", null
        );
        machine.setStatus(MachineStatus.REVOKED, Instant.now());

        assertThatThrownBy(() -> machine.activate(Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(Machine.ErrorMessages.REVOKED_MACHINE_CANNOT_BE_REACTIVATED);
    }

    @Test
    void completeEnrollment_transitionsToActive() {
        Machine machine = new Machine(
                UUID.randomUUID(), "WP-1", "Godzilla", REGISTERED_AT, UUID.randomUUID(), "nonce", null
        );

        machine.completeEnrollment(Instant.now());

        assertThat(machine.getStatus()).isEqualTo(MachineStatus.ACTIVE);
    }
}

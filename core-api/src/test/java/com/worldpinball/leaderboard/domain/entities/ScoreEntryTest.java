package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.events.ScoreSubmittedDomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ScoreEntry Domain Entity Unit Tests")
class ScoreEntryTest {

    @Test
    void constructor_whenValid_initializesAndEmitsScoreSubmittedDomainEvent() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID machineId = UUID.randomUUID();
        UUID ipId = UUID.randomUUID();
        UUID modeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        ScoreEntry entry = new ScoreEntry(
                sessionId, 1, deviceId, machineId, ipId, modeId, 12_500_000L, now, userId
        );

        assertThat(entry.getScoreEntryId()).isNotNull();
        assertThat(entry.getScore()).isEqualTo(12_500_000L);
        assertThat(entry.isValidated()).isTrue();
        assertThat(entry.getDomainEvents()).hasSize(1);
        assertThat(entry.getDomainEvents().getFirst()).isInstanceOf(ScoreSubmittedDomainEvent.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5, -1})
    void constructor_whenSlotNumberOutOfRange_throwsIllegalArgumentException(int invalidSlot) {
        assertThatThrownBy(() -> new ScoreEntry(
                UUID.randomUUID(), invalidSlot, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), 100_000L, Instant.now(), null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage(ScoreEntry.ErrorMessages.INVALID_SLOT_NUMBER);
    }

    @Test
    void constructor_whenScoreNegative_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new ScoreEntry(
                UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), -1L, Instant.now(), null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage(ScoreEntry.ErrorMessages.INVALID_SCORE);
    }

    @Test
    void invalidate_marksInvalidatedAndStoresReason() {
        ScoreEntry entry = new ScoreEntry(
                UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), 100_000L, Instant.now(), null
        );

        entry.invalidate("Tilt triggered abnormally.");

        assertThat(entry.isValidated()).isFalse();
        assertThat(entry.getValidationReason()).isEqualTo("Tilt triggered abnormally.");
    }
}

package com.worldpinball.leaderboard.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OutboxMessage Domain Entity Unit Tests")
class OutboxMessageTest {

    @Test
    void markAsProcessed_andRecordFailure_updateStateCorrectly() {
        Instant occurredAt = Instant.now();
        OutboxMessage msg = new OutboxMessage(
                UUID.randomUUID(),
                "ScoreSubmittedDomainEvent",
                "{\"score\":1000}",
                occurredAt
        );

        msg.recordFailure("Broker timeout");
        assertThat(msg.getRetryCount()).isEqualTo(1);
        assertThat(msg.getError()).isEqualTo("Broker timeout");
        assertThat(msg.getProcessedAtUtc()).isNull();

        Instant processedAt = occurredAt.plusSeconds(2);
        msg.markAsProcessed(processedAt);
        assertThat(msg.getProcessedAtUtc()).isEqualTo(processedAt);
        assertThat(msg.getError()).isNull();
    }
}

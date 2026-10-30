package com.worldpinball.leaderboard.infrastructure.outbox;

import com.worldpinball.leaderboard.application.services.EventPublisher;
import com.worldpinball.leaderboard.domain.entities.OutboxMessage;
import com.worldpinball.leaderboard.infrastructure.OutboxMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxProcessor Unit Tests")
class OutboxProcessorTest {

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Test
    @DisplayName("OutboxProcessor publishes unprocessed messages and records failures on exception")
    void outboxProcessor_publishesAndHandlesFailures() {
        Instant fixedNow = Instant.parse("2026-10-18T08:00:00Z");
        Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

        OutboxMessage msgOk = new OutboxMessage(UUID.randomUUID(), "ScoreSubmitted", "{\"ok\":true}", fixedNow);
        OutboxMessage msgFail = new OutboxMessage(UUID.randomUUID(), "ScoreSubmitted", "{\"ok\":false}", fixedNow);

        when(outboxMessageRepository.findUnprocessedBatchForUpdate(20))
                .thenReturn(List.of(msgOk, msgFail));

        lenient().doThrow(new RuntimeException("Kafka broker unreachable"))
                .when(eventPublisher)
                .publish(eq("game-sessions"), eq(msgFail.getId().toString()), any());

        OutboxProcessor processor = new OutboxProcessor(outboxMessageRepository, eventPublisher, fixedClock);
        int processed = processor.processBatch();

        assertThat(processed).isEqualTo(2);
        assertThat(msgOk.getProcessedAtUtc()).isEqualTo(fixedNow);
        assertThat(msgOk.getError()).isNull();

        assertThat(msgFail.getProcessedAtUtc()).isNull();
        assertThat(msgFail.getRetryCount()).isEqualTo(1);
        assertThat(msgFail.getError()).contains("Kafka broker unreachable");
    }
}

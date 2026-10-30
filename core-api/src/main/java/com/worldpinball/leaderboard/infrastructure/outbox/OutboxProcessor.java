package com.worldpinball.leaderboard.infrastructure.outbox;

import com.worldpinball.leaderboard.application.services.EventPublisher;
import com.worldpinball.leaderboard.common.EventId;
import com.worldpinball.leaderboard.domain.entities.OutboxMessage;
import com.worldpinball.leaderboard.infrastructure.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private static final EventId PUBLISH_FAILED = new EventId(4001, "OutboxPublishFailed");
    private static final int BATCH_SIZE = 20;

    private final OutboxMessageRepository outboxMessageRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public int processBatch() {
        List<OutboxMessage> messages = outboxMessageRepository.findUnprocessedBatchForUpdate(BATCH_SIZE);
        if (messages.isEmpty()) {
            return 0;
        }

        Instant nowUtc = Instant.now(clock);

        for (OutboxMessage message : messages) {
            try {
                eventPublisher.publish(
                        "game-sessions",
                        message.getId().toString(),
                        message.getContent()
                );
                message.markAsProcessed(nowUtc);
            } catch (Exception ex) {
                log.error("{} Failed to publish Outbox message {}",
                        PUBLISH_FAILED.tag(), message.getId(), ex);
                message.recordFailure(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            }
        }

        outboxMessageRepository.saveAll(messages);
        return messages.size();
    }
}

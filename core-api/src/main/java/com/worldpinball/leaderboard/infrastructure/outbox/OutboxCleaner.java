package com.worldpinball.leaderboard.infrastructure.outbox;

import com.worldpinball.leaderboard.domain.entities.OutboxMessage;
import com.worldpinball.leaderboard.infrastructure.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxCleaner {

    private final OutboxMessageRepository outboxMessageRepository;
    private final Clock clock;

    @Transactional
    public int cleanupBatch(Duration retentionPeriod) {
        return cleanupBatch(retentionPeriod, 100);
    }

    @Transactional
    public int cleanupBatch(Duration retentionPeriod, int batchSize) {
        Instant cutoff = Instant.now(clock).minus(retentionPeriod);
        List<OutboxMessage> toDelete = outboxMessageRepository.findProcessedBefore(
                cutoff,
                PageRequest.of(0, batchSize)
        );

        if (toDelete.isEmpty()) {
            return 0;
        }

        outboxMessageRepository.deleteAllInBatch(toDelete);
        return toDelete.size();
    }
}

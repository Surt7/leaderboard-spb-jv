package com.worldpinball.leaderboard.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldpinball.leaderboard.application.services.OutboxNotificationChannel;
import com.worldpinball.leaderboard.domain.entities.BaseEntity;
import com.worldpinball.leaderboard.domain.entities.OutboxMessage;
import com.worldpinball.leaderboard.domain.events.DomainEvent;
import com.worldpinball.leaderboard.infrastructure.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mirrors C# ConvertDomainEventsToOutboxMessagesInterceptor:
 * Converts pending domain events on aggregate entities into OutboxMessage records
 * within the same database transaction, and notifies OutboxNotificationChannel only after commit.
 */
@Component
@RequiredArgsConstructor
public class DomainEventOutboxRelay {

    private final OutboxMessageRepository outboxMessageRepository;
    private final OutboxNotificationChannel outboxNotificationChannel;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public void persistOutboxMessagesAndNotifyAfterCommit(BaseEntity entity) {
        if (entity == null || entity.getDomainEvents().isEmpty()) {
            return;
        }

        List<DomainEvent> events = new ArrayList<>(entity.getDomainEvents());
        entity.clearDomainEvents();

        Instant nowUtc = Instant.now(clock);
        List<OutboxMessage> outboxMessages = new ArrayList<>(events.size());

        for (DomainEvent domainEvent : events) {
            try {
                String payload = objectMapper.writeValueAsString(domainEvent);
                outboxMessages.add(new OutboxMessage(
                        UUID.randomUUID(),
                        domainEvent.getClass().getSimpleName(),
                        payload,
                        nowUtc
                ));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize domain event " + domainEvent.getClass().getSimpleName(), e);
            }
        }

        outboxMessageRepository.saveAll(outboxMessages);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    outboxNotificationChannel.tryNotify();
                }
            });
        } else {
            outboxNotificationChannel.tryNotify();
        }
    }
}

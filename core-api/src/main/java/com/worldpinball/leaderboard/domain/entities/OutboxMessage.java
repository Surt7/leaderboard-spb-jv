package com.worldpinball.leaderboard.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxMessage {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "type", length = 255, nullable = false)
    private String type;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "occurred_at_utc", nullable = false)
    private Instant occurredAtUtc;

    @Column(name = "processed_at_utc")
    private Instant processedAtUtc;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    public OutboxMessage(UUID id, String type, String content, Instant occurredAtUtc) {
        if (id == null) {
            throw new IllegalArgumentException("Outbox message ID cannot be null.");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Outbox message type cannot be empty.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Outbox message content cannot be empty.");
        }
        this.id = id;
        this.type = type;
        this.content = content;
        this.occurredAtUtc = occurredAtUtc != null ? occurredAtUtc : Instant.now();
        this.retryCount = 0;
    }

    public void markAsProcessed(Instant nowUtc) {
        this.processedAtUtc = nowUtc != null ? nowUtc : Instant.now();
        this.error = null;
    }

    public void recordFailure(String errorMessage) {
        this.error = errorMessage;
        this.retryCount++;
    }
}

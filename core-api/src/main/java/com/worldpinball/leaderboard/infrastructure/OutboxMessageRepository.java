package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.OutboxMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {

    @Query(value = """
            SELECT *
            FROM outbox_messages
            WHERE processed_at_utc IS NULL AND retry_count < 5
            ORDER BY occurred_at_utc
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxMessage> findUnprocessedBatchForUpdate(@Param("batchSize") int batchSize);

    @Query("""
            SELECT m
            FROM OutboxMessage m
            WHERE m.processedAtUtc IS NOT NULL AND m.processedAtUtc <= :cutoff
            ORDER BY m.processedAtUtc ASC
            """)
    List<OutboxMessage> findProcessedBefore(@Param("cutoff") Instant cutoff, Pageable pageable);
}

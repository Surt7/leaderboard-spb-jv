package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.ScoreEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScoreEntryRepository extends JpaRepository<ScoreEntry, UUID> {

    Optional<ScoreEntry> findBySessionIdAndSlotNumber(UUID sessionID, int slotNumber);

    long countBySessionId(UUID sessionId);

    long countByUserId(UUID userId);

    @Query("SELECT COALESCE(MAX(s.score), 0) FROM ScoreEntry s WHERE s.userId = :userId")
    long findMaxScoreByUserId(@Param("userId") UUID userId);

    long countByUserIdAndModeId(UUID userId, UUID modeId);

    @Query("SELECT COALESCE(MAX(s.score), 0) FROM ScoreEntry s WHERE s.userId = :userId AND s.modeId = :modeId")
    long findMaxScoreByUserIdAndModeId(@Param("userId") UUID userId, @Param("modeId") UUID modeId);

    List<ScoreEntry> findByUserId(UUID userId);

    @Query("""
            SELECT s FROM ScoreEntry s
            WHERE s.ipId= :ipId
            AND s.modeId = :modeId
            AND s.validated = true
            ORDER BY s.score DESC, s.achievedAtUtc ASC
            """)
    List<ScoreEntry> findTopScoresByIpAndMode(
            @Param("ipId") UUID ipId,
            @Param("modeId") UUID modeId,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(s) + 1 FROM ScoreEntry s
        WHERE s.ipId = :ipId
          AND s.modeId = :modeId
          AND s.validated = true
          AND s.score > :score
    """)
    long calculateRank(
            @Param("ipId") UUID ipId,
            @Param("modeId") UUID modeId,
            @Param("score") long score
    );
}

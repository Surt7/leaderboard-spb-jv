package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

    @Query("SELECT gs FROM GameSession gs LEFT JOIN FETCH gs.players WHERE gs.sessionId = :sessionId")
    Optional<GameSession> findByIdWithPlayers(@Param("sessionId") UUID sessionId);

    @Query("SELECT gs.sessionId FROM GameSession gs WHERE gs.state NOT IN ('ENDED', 'CANCELLED', 'ABANDONED', 'ABORTED') ORDER BY gs.startedAtUtc DESC")
    List<UUID> findActiveSessionIds();

    @Query("SELECT DISTINCT gs FROM GameSession gs LEFT JOIN FETCH gs.players p WHERE p.userId = :userId AND gs.state NOT IN ('ENDED', 'CANCELLED') ORDER BY gs.startedAtUtc DESC")
    List<GameSession> findActiveSessionsForUser(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT gs FROM GameSession gs LEFT JOIN FETCH gs.players WHERE gs.machineId IN :machineIds AND gs.state NOT IN ('ENDED', 'CANCELLED') ORDER BY gs.startedAtUtc DESC")
    List<GameSession> findActiveSessionsByMachineIds(@Param("machineIds") List<UUID> machineIds);
}

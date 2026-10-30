package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.GameSessionPlayer;
import com.worldpinball.leaderboard.domain.entities.GameSessionPlayerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameSessionPlayerRepository extends JpaRepository<GameSessionPlayer, GameSessionPlayerId> {
    List<GameSessionPlayer> findBySessionId(UUID sessionId);
    Optional<GameSessionPlayer> findBySessionIdAndSlotNumber(UUID sessionId, Integer slotNumber);
}

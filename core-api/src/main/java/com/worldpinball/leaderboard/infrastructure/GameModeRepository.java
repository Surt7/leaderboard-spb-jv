package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameModeRepository extends JpaRepository<GameMode, UUID> {
    Optional<GameMode> findByCode(String code);
    Optional<GameMode> findByCodeIgnoreCaseAndActiveTrue(String code);
    List<GameMode> findAllByActiveTrueOrderByNameAsc();
}

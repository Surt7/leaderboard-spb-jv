package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.OperatorToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OperatorTokenRepository extends JpaRepository<OperatorToken, UUID> {
    Optional<OperatorToken> findByTokenHash(String tokenHash);
    Optional<OperatorToken> findByTokenHashAndActiveTrue(String tokenHash);
}

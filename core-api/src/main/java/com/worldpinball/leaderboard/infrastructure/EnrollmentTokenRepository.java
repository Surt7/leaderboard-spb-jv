package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.EnrollmentToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentTokenRepository extends JpaRepository<EnrollmentToken, UUID> {
    Optional<EnrollmentToken> findByTokenHash(String tokenHash);
}

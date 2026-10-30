package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.StreamAccessGrant;
import com.worldpinball.leaderboard.domain.enums.GrantStatus;
import com.worldpinball.leaderboard.domain.enums.PrincipalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StreamAccessGrantRepository extends JpaRepository<StreamAccessGrant, UUID> {
    Optional<StreamAccessGrant> findByPrincipalTypeAndPrincipalIdAndStatus(
            PrincipalType principalType,
            UUID principalId,
            GrantStatus status
    );

    List<StreamAccessGrant> findAllByOrderByCreatedAtUtcDesc();
}

package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.CaCertificate;
import com.worldpinball.leaderboard.domain.enums.CaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaCertificateRepository extends JpaRepository<CaCertificate, UUID> {
    Optional<CaCertificate> findFirstByCaTypeAndActiveTrue(CaType caType);
    Optional<CaCertificate> findFirstByActiveTrueOrderByCreatedAtDesc();
}

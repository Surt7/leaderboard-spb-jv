package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.CertificateRevocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRevocationRepository extends JpaRepository<CertificateRevocation, UUID> {
    Optional<CertificateRevocation> findByDeviceCertId(UUID deviceCertId);
}

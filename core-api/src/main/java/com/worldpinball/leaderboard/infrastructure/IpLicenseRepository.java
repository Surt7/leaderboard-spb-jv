package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.IpLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpLicenseRepository extends JpaRepository<IpLicense, UUID> {
    Optional<IpLicense> findFirstBySlugIgnoreCaseAndActiveTrue(String slug);

    default Optional<IpLicense> findBySlugIgnoreCaseAndActiveTrue(String slug) {
        return findFirstBySlugIgnoreCaseAndActiveTrue(slug);
    }

    List<IpLicense> findAllByActiveTrueOrderByNameAsc();
}

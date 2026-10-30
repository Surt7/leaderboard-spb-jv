package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.DeviceCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceCertificateRepository extends JpaRepository<DeviceCertificate, UUID> {
    List<DeviceCertificate> findByDeviceIdOrderByNotAfterUtcDesc(UUID deviceId);
    Optional<DeviceCertificate> findFirstByDeviceIdOrderByNotAfterUtcDesc(UUID deviceId);
    Optional<DeviceCertificate> findFirstByDeviceIdOrderByIssuedAtUtcDesc(UUID deviceId);
}

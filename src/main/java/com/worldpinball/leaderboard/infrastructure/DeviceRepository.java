package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    boolean existsBySerialPhysical(String serialPhysical);
    Optional<Device> findBySerialPhysical(String serialPhysical);
    List<Device> findByOwnerUserId(UUID ownerUserId);
}

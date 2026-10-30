package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.MachineDevice;
import com.worldpinball.leaderboard.domain.entities.MachineDeviceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MachineDeviceRepository extends JpaRepository<MachineDevice, MachineDeviceId> {
    Optional<MachineDevice> findByDeviceIdAndActiveTrue(UUID deviceId);
    Optional<MachineDevice> findFirstByDeviceIdAndActiveTrue(UUID deviceId);
    Optional<MachineDevice> findFirstByMachineIdAndActiveTrue(UUID machineId);
    List<MachineDevice> findByDeviceIdInAndActiveTrue(Collection<UUID> deviceIds);
}

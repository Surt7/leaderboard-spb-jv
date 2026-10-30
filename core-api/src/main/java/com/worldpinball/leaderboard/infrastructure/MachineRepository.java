package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MachineRepository extends JpaRepository<Machine, UUID> {
    boolean existsByWorldPinballSerialNumber(String serialNumber);
    boolean existsByIdempotencyKey(UUID idempotencyKey);
    Optional<Machine> findByWorldPinballSerialNumber(String serialNumber);
}

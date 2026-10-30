package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.RolePermission;
import com.worldpinball.leaderboard.domain.entities.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    boolean existsByRoleAndPermission(String role, String permission);
    void deleteByRoleAndPermission(String role, String permission);
}

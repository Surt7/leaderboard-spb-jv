package com.worldpinball.leaderboard.infrastructure.security.authorization;

import com.worldpinball.leaderboard.application.services.RolePermissionReloader;
import com.worldpinball.leaderboard.application.services.RolePermissionStore;
import com.worldpinball.leaderboard.domain.entities.RolePermission;
import com.worldpinball.leaderboard.infrastructure.RolePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mirrors C# DynamicRolePermissionStore:
 * Lock-free atomic pointer swap via volatile reference to an immutable FrozenRolePermissionStore.
 */
@Component
public class DynamicRolePermissionStore implements RolePermissionStore, RolePermissionReloader {

    private volatile FrozenRolePermissionStore current;
    private final RolePermissionRepository rolePermissionRepository;

    @Autowired
    public DynamicRolePermissionStore(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.current = new FrozenRolePermissionStore(Map.of());
    }

    public DynamicRolePermissionStore(Map<String, ? extends Collection<String>> initial) {
        this.rolePermissionRepository = null;
        this.current = new FrozenRolePermissionStore(initial);
    }

    @Override
    public void reload(Map<String, ? extends Collection<String>> data) {
        this.current = new FrozenRolePermissionStore(data);
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void reloadFromDatabase() {
        if (rolePermissionRepository == null) {
            return;
        }
        try {
            List<RolePermission> list = rolePermissionRepository.findAll();
            Map<String, List<String>> grouped = list.stream()
                    .collect(Collectors.groupingBy(
                            RolePermission::getRole,
                            Collectors.mapping(RolePermission::getPermission, Collectors.toList())
                    ));
            reload(grouped);
        } catch (Exception ignored) {
            // Resilient fallback: keep existing in-memory store if DB is not ready
        }
    }

    @Override
    public boolean hasPermission(String role, String permission) {
        if (current.isEmpty() && rolePermissionRepository != null) {
            reloadFromDatabase();
        }
        return current.hasPermission(role, permission);
    }

    @Override
    public boolean hasPermission(Collection<String> roles, String permission) {
        if (current.isEmpty() && rolePermissionRepository != null) {
            reloadFromDatabase();
        }
        return current.hasPermission(roles, permission);
    }

    @Override
    public Set<String> getPermissions(String role) {
        if (current.isEmpty() && rolePermissionRepository != null) {
            reloadFromDatabase();
        }
        return current.getPermissions(role);
    }
}

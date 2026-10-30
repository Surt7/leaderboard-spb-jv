package com.worldpinball.leaderboard.infrastructure.security.authorization;

import com.worldpinball.leaderboard.application.services.RolePermissionStore;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Mirrors C# FrozenRolePermissionStore:
 * Immutable, unmodifiable L1 lookup table with case-insensitive role & permission matching.
 */
public final class FrozenRolePermissionStore implements RolePermissionStore {

    private final Map<String, Set<String>> rolePermissionsLower;
    private final Map<String, Set<String>> rolePermissionsOriginal;

    public FrozenRolePermissionStore(Map<String, ? extends Collection<String>> rolePermissions) {
        if (rolePermissions == null || rolePermissions.isEmpty()) {
            this.rolePermissionsLower = Map.of();
            this.rolePermissionsOriginal = Map.of();
            return;
        }

        Map<String, Set<String>> lowerMap = new HashMap<>();
        Map<String, Set<String>> origMap = new HashMap<>();

        for (Map.Entry<String, ? extends Collection<String>> entry : rolePermissions.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String roleLower = entry.getKey().toLowerCase(Locale.ROOT);
            Set<String> permsLower = new HashSet<>( lowerMap.getOrDefault(roleLower, Set.of()) );
            Set<String> permsOrig = new HashSet<>( origMap.getOrDefault(roleLower, Set.of()) );

            for (String perm : entry.getValue()) {
                if (perm != null) {
                    permsLower.add(perm.toLowerCase(Locale.ROOT));
                    permsOrig.add(perm);
                }
            }
            lowerMap.put(roleLower, Set.copyOf(permsLower));
            origMap.put(roleLower, Set.copyOf(permsOrig));
        }

        this.rolePermissionsLower = Map.copyOf(lowerMap);
        this.rolePermissionsOriginal = Map.copyOf(origMap);
    }

    @Override
    public boolean hasPermission(String role, String permission) {
        if (role == null || permission == null) {
            return false;
        }
        Set<String> perms = rolePermissionsLower.get(role.toLowerCase(Locale.ROOT));
        return perms != null && perms.contains(permission.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean hasPermission(Collection<String> roles, String permission) {
        if (roles == null || permission == null) {
            return false;
        }
        for (String role : roles) {
            if (hasPermission(role, permission)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return rolePermissionsLower.isEmpty();
    }

    @Override
    public Set<String> getPermissions(String role) {
        if (role == null) {
            return Collections.emptySet();
        }
        return rolePermissionsOriginal.getOrDefault(role.toLowerCase(Locale.ROOT), Collections.emptySet());
    }
}

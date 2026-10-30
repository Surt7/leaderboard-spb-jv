package com.worldpinball.leaderboard.application.services;

import java.util.Collection;
import java.util.Set;

public interface RolePermissionStore {
    boolean hasPermission(String role, String permission);
    boolean hasPermission(Collection<String> roles, String permission);
    Set<String> getPermissions(String role);
}

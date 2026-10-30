package com.worldpinball.leaderboard.infrastructure.security.authorization;

import com.worldpinball.leaderboard.infrastructure.security.authentication.ClaimsPrincipal;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Evaluates fine-grained permission requirements:
 * 1. Succeeds immediately if caller is in role {@code Admin}.
 * 2. Succeeds if caller has direct {@code permission} or {@code permissions} claim matching the permission.
 * 3. Otherwise delegates role lookup to {@link DynamicRolePermissionStore}.
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.Authorization.PermissionAuthorizationHandler}.
 */
@Component
public class PermissionAuthorizationHandler {

    private final DynamicRolePermissionStore store;

    public PermissionAuthorizationHandler(DynamicRolePermissionStore store) {
        this.store = store;
    }

    public boolean isAuthorized(ClaimsPrincipal user, String requiredPermission) {
        if (user == null || requiredPermission == null || requiredPermission.isBlank()) {
            return false;
        }

        if (user.isInRole("Admin")) {
            return true;
        }

        if (user.hasClaim(c -> ("permission".equalsIgnoreCase(c.type()) || "permissions".equalsIgnoreCase(c.type()))
                && c.value().equalsIgnoreCase(requiredPermission))) {
            return true;
        }

        List<String> roles = user.findAll(c ->
                ClaimsPrincipal.CLAIM_ROLE.equalsIgnoreCase(c.type())
                        || "roles".equalsIgnoreCase(c.type())
        );

        for (String role : roles) {
            if (role.equalsIgnoreCase(requiredPermission)
                    || ("MachinesProvision".equalsIgnoreCase(requiredPermission) && "MachineProvision".equalsIgnoreCase(role))) {
                return true;
            }
        }

        if (user.hasClaim(c -> "scope".equalsIgnoreCase(c.type()) && "*".equals(c.value()))) {
            return true;
        }

        return store.hasPermission(roles, requiredPermission);
    }
}

package com.worldpinball.leaderboard.infrastructure.security.authorization;

import com.worldpinball.leaderboard.infrastructure.security.authentication.ClaimsPrincipal;
import com.worldpinball.leaderboard.infrastructure.security.authentication.MachineClaimsTransformation;
import org.springframework.stereotype.Component;

/**
 * Verifies that the authenticated machine has {@code machine_status == "Active"}.
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.Authorization.ActiveMachineAuthorizationHandler}.
 */
@Component
public class ActiveMachineAuthorizationHandler {

    public boolean isAuthorized(ClaimsPrincipal user) {
        if (user == null) {
            return false;
        }
        return user.findFirst(MachineClaimsTransformation.CLAIM_MACHINE_STATUS)
                .map(status -> "Active".equalsIgnoreCase(status))
                .orElse(false);
    }
}

package com.worldpinball.leaderboard.infrastructure.security.authorization;

import com.worldpinball.leaderboard.infrastructure.security.authentication.ClaimsPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PermissionAuthorizationHandler Tests")
class PermissionAuthorizationHandlerTest {

    @Test
    @DisplayName("Succeeds when user's role grants the requested permission")
    void handle_whenRoleHasPermission_succeeds() {
        DynamicRolePermissionStore store = new DynamicRolePermissionStore(Map.of(
                "Technician", List.of("MachinesProvision")
        ));
        PermissionAuthorizationHandler sut = new PermissionAuthorizationHandler(store);

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "User",
                List.of(),
                List.of("Technician")
        );

        boolean allowed = sut.isAuthorized(principal, "MachinesProvision");
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Fails when user's role does not grant the requested permission")
    void handle_whenRoleLacksPermission_fails() {
        DynamicRolePermissionStore store = new DynamicRolePermissionStore(Map.of(
                "Player", List.of("ScoresRead")
        ));
        PermissionAuthorizationHandler sut = new PermissionAuthorizationHandler(store);

        ClaimsPrincipal principal = new ClaimsPrincipal(
                "User",
                List.of(),
                List.of("Player")
        );

        boolean allowed = sut.isAuthorized(principal, "MachinesProvision");
        assertThat(allowed).isFalse();
    }
}

package com.worldpinball.leaderboard.application.features.roles.Admin;

import com.worldpinball.leaderboard.application.features.roles.Admin.grantpermission.GrantPermissionToRoleCommand;
import com.worldpinball.leaderboard.application.features.roles.Admin.grantpermission.GrantPermissionToRoleHandlerImpl;
import com.worldpinball.leaderboard.application.features.roles.Admin.grantpermission.GrantPermissionToRoleResult;
import com.worldpinball.leaderboard.application.features.roles.Admin.revokepermission.RevokePermissionFromRoleCommand;
import com.worldpinball.leaderboard.application.features.roles.Admin.revokepermission.RevokePermissionFromRoleHandlerImpl;
import com.worldpinball.leaderboard.application.features.roles.Admin.revokepermission.RevokePermissionFromRoleResult;
import com.worldpinball.leaderboard.application.services.RolePermissionReloader;
import com.worldpinball.leaderboard.domain.entities.RolePermission;
import com.worldpinball.leaderboard.infrastructure.RolePermissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Grant and Revoke Permission Role Handler Tests")
class GrantAndRevokePermissionHandlerTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private RolePermissionReloader rolePermissionReloader;

    @Test
    @DisplayName("GrantPermissionToRoleHandler grants new permission and reloads store")
    void grantPermissionToRoleHandler_grantsAndReloads() {
        when(rolePermissionRepository.existsByRoleAndPermission("Technician", "MachinesProvision"))
                .thenReturn(false);

        GrantPermissionToRoleHandlerImpl handler = new GrantPermissionToRoleHandlerImpl(
                rolePermissionRepository, rolePermissionReloader
        );

        GrantPermissionToRoleResult result = handler.handle(
                new GrantPermissionToRoleCommand("Technician", "MachinesProvision")
        );

        assertThat(result).isInstanceOf(GrantPermissionToRoleResult.Success.class);
        verify(rolePermissionRepository).save(any());
        verify(rolePermissionReloader).reloadFromDatabase();
    }

    @Test
    @DisplayName("RevokePermissionFromRoleHandler revokes existing permission and reloads store")
    void revokePermissionFromRoleHandler_revokesAndReloads() {
        when(rolePermissionRepository.existsByRoleAndPermission("Technician", "MachinesProvision"))
                .thenReturn(true);

        RevokePermissionFromRoleHandlerImpl handler = new RevokePermissionFromRoleHandlerImpl(
                rolePermissionRepository, rolePermissionReloader
        );

        RevokePermissionFromRoleResult result = handler.handle(
                new RevokePermissionFromRoleCommand("Technician", "MachinesProvision")
        );

        assertThat(result).isInstanceOf(RevokePermissionFromRoleResult.Success.class);
        verify(rolePermissionRepository).deleteByRoleAndPermission("Technician", "MachinesProvision");
        verify(rolePermissionReloader).reloadFromDatabase();
    }
}

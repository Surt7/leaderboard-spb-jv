package com.worldpinball.leaderboard.application.features.roles.Admin;

import com.worldpinball.leaderboard.application.features.roles.Admin.revokepermission.RevokePermissionFromRoleCommand;
import com.worldpinball.leaderboard.application.features.roles.Admin.revokepermission.RevokePermissionFromRoleHandlerImpl;
import com.worldpinball.leaderboard.application.features.roles.Admin.revokepermission.RevokePermissionFromRoleResult;
import com.worldpinball.leaderboard.application.services.RolePermissionReloader;
import com.worldpinball.leaderboard.infrastructure.RolePermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RevokePermissionFromRoleHandler Tests")
class RevokePermissionFromRoleHandlerTest {

    @Mock
    private RolePermissionRepository repository;

    @Mock
    private RolePermissionReloader reloader;

    private RevokePermissionFromRoleHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new RevokePermissionFromRoleHandlerImpl(repository, reloader);
    }

    @Test
    @DisplayName("Deletes permission and reloads when permission exists for role")
    void handle_whenExisting_shouldRemoveReloadAndReturnSuccess() {
        when(repository.existsByRoleAndPermission("Technician", "MachinesProvision")).thenReturn(true);

        RevokePermissionFromRoleResult result = sut.handle(
                new RevokePermissionFromRoleCommand("Technician", "MachinesProvision")
        );

        assertThat(result).isInstanceOf(RevokePermissionFromRoleResult.Success.class);
        verify(repository).deleteByRoleAndPermission("Technician", "MachinesProvision");
        verify(reloader).reloadFromDatabase();
    }

    @Test
    @DisplayName("Returns NotFound and does not delete when permission does not exist for role")
    void handle_whenNotFound_shouldReturnNotFoundAndNotRemove() {
        when(repository.existsByRoleAndPermission("Technician", "MachinesProvision")).thenReturn(false);

        RevokePermissionFromRoleResult result = sut.handle(
                new RevokePermissionFromRoleCommand("Technician", "MachinesProvision")
        );

        assertThat(result).isInstanceOf(RevokePermissionFromRoleResult.NotFound.class);
        verify(repository, never()).deleteByRoleAndPermission(any(), any());
        verifyNoInteractions(reloader);
    }
}

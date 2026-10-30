package com.worldpinball.leaderboard.application.features.roles.Admin;

import com.worldpinball.leaderboard.application.features.roles.Admin.grantpermission.GrantPermissionToRoleCommand;
import com.worldpinball.leaderboard.application.features.roles.Admin.grantpermission.GrantPermissionToRoleHandlerImpl;
import com.worldpinball.leaderboard.application.features.roles.Admin.grantpermission.GrantPermissionToRoleResult;
import com.worldpinball.leaderboard.application.services.RolePermissionReloader;
import com.worldpinball.leaderboard.domain.entities.RolePermission;
import com.worldpinball.leaderboard.infrastructure.RolePermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GrantPermissionToRoleHandler Tests")
class GrantPermissionToRoleHandlerTest {

    @Mock
    private RolePermissionRepository repository;

    @Mock
    private RolePermissionReloader reloader;

    private GrantPermissionToRoleHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new GrantPermissionToRoleHandlerImpl(repository, reloader);
    }

    @Test
    @DisplayName("Saves new permission and reloads when permission does not exist for role")
    void handle_whenValidAndNotExisting_shouldPersistReloadAndReturnSuccess() {
        when(repository.existsByRoleAndPermission("Technician", "MachinesProvision")).thenReturn(false);

        GrantPermissionToRoleResult result = sut.handle(
                new GrantPermissionToRoleCommand("Technician", "MachinesProvision")
        );

        assertThat(result).isInstanceOf(GrantPermissionToRoleResult.Success.class);

        ArgumentCaptor<RolePermission> captor = ArgumentCaptor.forClass(RolePermission.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("Technician");
        assertThat(captor.getValue().getPermission()).isEqualTo("MachinesProvision");

        verify(reloader).reloadFromDatabase();
    }

    @Test
    @DisplayName("Returns AlreadyExists and does not persist when permission already exists for role")
    void handle_whenAlreadyExists_shouldReturnAlreadyExistsAndNotPersist() {
        when(repository.existsByRoleAndPermission("Technician", "MachinesProvision")).thenReturn(true);

        GrantPermissionToRoleResult result = sut.handle(
                new GrantPermissionToRoleCommand("Technician", "MachinesProvision")
        );

        assertThat(result).isInstanceOf(GrantPermissionToRoleResult.AlreadyExists.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(reloader);
    }
}

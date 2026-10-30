package com.worldpinball.leaderboard.infrastructure.security.authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RolePermissionStore Tests")
class RolePermissionStoreTest {

    @Test
    @DisplayName("FrozenRolePermissionStore matches role and permission case-insensitively")
    void frozenRolePermissionStore_matchesCaseInsensitively() {
        FrozenRolePermissionStore sut = new FrozenRolePermissionStore(Map.of(
                "Technician", List.of("MachinesProvision", "ShipmentsDispatch")
        ));

        assertThat(sut.hasPermission("Technician", "MachinesProvision")).isTrue();
        assertThat(sut.hasPermission("technician", "machinesprovision")).isTrue();
        assertThat(sut.hasPermission("Technician", "UnknownPerm")).isFalse();
        assertThat(sut.hasPermission("UnknownRole", "MachinesProvision")).isFalse();
        assertThat(sut.hasPermission(List.of("Player", "Technician"), "MachinesProvision")).isTrue();
    }

    @Test
    @DisplayName("DynamicRolePermissionStore reloads permissions atomically")
    void dynamicRolePermissionStore_reloadsAtomically() {
        DynamicRolePermissionStore sut = new DynamicRolePermissionStore(Map.of(
                "Technician", List.of("MachinesProvision")
        ));

        assertThat(sut.hasPermission("Technician", "ShipmentsDispatch")).isFalse();

        sut.reload(Map.of(
                "Technician", List.of("MachinesProvision", "ShipmentsDispatch")
        ));

        assertThat(sut.hasPermission("Technician", "ShipmentsDispatch")).isTrue();
    }
}

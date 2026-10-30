package com.worldpinball.leaderboard.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "role_permissions")
@IdClass(RolePermissionId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RolePermission {

    @Id
    @Column(name = "role", length = 100, nullable = false)
    private String role;

    @Id
    @Column(name = "permission", length = 100, nullable = false)
    private String permission;

    public RolePermission(String role, String permission) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be empty.");
        }
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("Permission cannot be empty.");
        }
        this.role = role;
        this.permission = permission;
    }
}

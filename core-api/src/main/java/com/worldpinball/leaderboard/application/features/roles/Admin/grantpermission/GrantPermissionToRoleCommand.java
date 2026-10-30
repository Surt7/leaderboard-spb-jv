package com.worldpinball.leaderboard.application.features.roles.Admin.grantpermission;

public record GrantPermissionToRoleCommand(
        String role,
        String permission
) {
}

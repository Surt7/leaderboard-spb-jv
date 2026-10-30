package com.worldpinball.leaderboard.application.features.roles.Admin.revokepermission;

public record RevokePermissionFromRoleCommand(
        String role,
        String permission
) {
}

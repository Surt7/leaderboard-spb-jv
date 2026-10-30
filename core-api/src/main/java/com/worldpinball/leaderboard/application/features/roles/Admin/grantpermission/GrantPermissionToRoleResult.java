package com.worldpinball.leaderboard.application.features.roles.Admin.grantpermission;

public sealed interface GrantPermissionToRoleResult {
    record Success() implements GrantPermissionToRoleResult {}
    record AlreadyExists() implements GrantPermissionToRoleResult {}
}

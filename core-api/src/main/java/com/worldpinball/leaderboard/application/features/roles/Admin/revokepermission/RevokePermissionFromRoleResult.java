package com.worldpinball.leaderboard.application.features.roles.Admin.revokepermission;

public sealed interface RevokePermissionFromRoleResult {
    record Success() implements RevokePermissionFromRoleResult {}
    record NotFound() implements RevokePermissionFromRoleResult {}
}

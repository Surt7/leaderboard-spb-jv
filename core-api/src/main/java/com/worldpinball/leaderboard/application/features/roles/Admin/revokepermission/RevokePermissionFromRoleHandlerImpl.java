package com.worldpinball.leaderboard.application.features.roles.Admin.revokepermission;

import com.worldpinball.leaderboard.application.services.RolePermissionReloader;
import com.worldpinball.leaderboard.infrastructure.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevokePermissionFromRoleHandlerImpl implements RevokePermissionFromRoleHandler {

    private final RolePermissionRepository repository;
    private final RolePermissionReloader reloader;

    @Override
    @Transactional
    public RevokePermissionFromRoleResult handle(RevokePermissionFromRoleCommand command) {
        if (!repository.existsByRoleAndPermission(command.role(), command.permission())) {
            return new RevokePermissionFromRoleResult.NotFound();
        }

        repository.deleteByRoleAndPermission(command.role(), command.permission());
        reloader.reloadFromDatabase();

        return new RevokePermissionFromRoleResult.Success();
    }
}

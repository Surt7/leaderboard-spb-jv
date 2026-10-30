package com.worldpinball.leaderboard.application.features.roles.Admin.grantpermission;

import com.worldpinball.leaderboard.application.services.RolePermissionReloader;
import com.worldpinball.leaderboard.domain.entities.RolePermission;
import com.worldpinball.leaderboard.infrastructure.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GrantPermissionToRoleHandlerImpl implements GrantPermissionToRoleHandler {

    private final RolePermissionRepository repository;
    private final RolePermissionReloader reloader;

    @Override
    @Transactional
    public GrantPermissionToRoleResult handle(GrantPermissionToRoleCommand command) {
        if (repository.existsByRoleAndPermission(command.role(), command.permission())) {
            return new GrantPermissionToRoleResult.AlreadyExists();
        }

        repository.save(new RolePermission(command.role(), command.permission()));
        reloader.reloadFromDatabase();

        return new GrantPermissionToRoleResult.Success();
    }
}

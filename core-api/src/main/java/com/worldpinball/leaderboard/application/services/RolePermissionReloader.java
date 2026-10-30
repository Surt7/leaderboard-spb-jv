package com.worldpinball.leaderboard.application.services;

import java.util.Collection;
import java.util.Map;

public interface RolePermissionReloader {
    void reload(Map<String, ? extends Collection<String>> data);
    void reloadFromDatabase();
}

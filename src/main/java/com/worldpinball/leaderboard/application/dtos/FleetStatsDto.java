package com.worldpinball.leaderboard.application.dtos;

public record FleetStatsDto(
        int total,
        int online,
        int offline,
        int maintenance,
        int pending
) {}

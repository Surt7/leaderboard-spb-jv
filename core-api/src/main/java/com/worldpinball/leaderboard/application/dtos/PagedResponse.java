package com.worldpinball.leaderboard.application.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record PagedResponse<T>(
        List<T> items,
        int pageSize,
        int totalCount,
        int pageNumber,
        boolean hasPreviousPage,
        boolean hasNextPage,
        UUID lastId,
        FleetStatsDto stats
) {
    @JsonProperty("totalPages")
    public int getTotalPages() {
        return pageSize > 0 ? (int) Math.ceil((double) totalCount / pageSize) : 0;
    }
}

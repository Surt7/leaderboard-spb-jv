package com.worldpinball.leaderboard.application.features.leaderboard.Public.getplayerrank;

public interface GetPlayerRankHandler {
    GetPlayerRankResult handle(GetPlayerRankQuery query);
}

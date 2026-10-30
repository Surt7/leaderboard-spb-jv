package com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.queries.GetLobbyQuery;

public interface GetLobbyHandler {
    GetLobbyResult handle(GetLobbyQuery query);
}

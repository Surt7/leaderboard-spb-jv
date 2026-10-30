package com.worldpinball.leaderboard.application.features.leaderboard.Public.getplayerrank;

import com.worldpinball.leaderboard.infrastructure.ScoreEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPlayerRankHandlerImpl implements GetPlayerRankHandler {

    private final ScoreEntryRepository scoreEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public GetPlayerRankResult handle(GetPlayerRankQuery query) {
        long rank = scoreEntryRepository.calculateRank(query.ipId(), query.modeId(), query.score());
        return new GetPlayerRankResult.Success(rank, query.score());
    }
}

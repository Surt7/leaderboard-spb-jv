package com.worldpinball.leaderboard.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldpinball.leaderboard.application.services.RealtimeLeaderboardService;
import com.worldpinball.leaderboard.domain.events.ScoreSubmittedDomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LeaderboardScoreEventProcessor {

    private final RealtimeLeaderboardService leaderboardService;
    private final ObjectMapper objectMapper;

    public void processMessage(String jsonPayload) {
        if (jsonPayload == null || jsonPayload.isBlank()) {
            return;
        }

        ScoreSubmittedDomainEvent evt;
        try {
            evt = objectMapper.readValue(jsonPayload, ScoreSubmittedDomainEvent.class);
        } catch (JsonProcessingException e) {
            return;
        }

        if (evt == null || evt.scoreEntryId() == null || evt.ipId() == null || evt.modeId() == null || evt.score() < 0) {
            return;
        }

        leaderboardService.addScore(evt.ipId(), evt.modeId(), evt.scoreEntryId(), evt.score());
    }
}

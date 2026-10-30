package com.worldpinball.leaderboard.api.controllers.Public;

import com.worldpinball.leaderboard.application.dto.SubmitScoreRequest;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getleaderboard.GetLeaderboardHandler;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getleaderboard.GetLeaderboardQuery;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getleaderboard.GetLeaderboardResult;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getplayerrank.GetPlayerRankHandler;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getplayerrank.GetPlayerRankQuery;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getplayerrank.GetPlayerRankResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.SubmitScoreHandler;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.SubmitScoreResult;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.commands.SubmitScoreCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leaderboards")
@RequiredArgsConstructor
public class LeaderboardController {

    private final SubmitScoreHandler submitScoreHandler;
    private final GetLeaderboardHandler getLeaderboardHandler;
    private final GetPlayerRankHandler getPlayerRankHandler;

    @GetMapping("/{ipId}/{modeId}")
    public ResponseEntity<?> getTopScores(
        @PathVariable UUID ipId,
        @PathVariable UUID modeId,
        @RequestParam(defaultValue = "10") int limit
    ) {
        GetLeaderboardResult result = getLeaderboardHandler.handle(
            new GetLeaderboardQuery(ipId, modeId, null, null, limit)
        );

        return switch (result) {
            case GetLeaderboardResult.Success s -> ResponseEntity.ok(s.rows());
            case GetLeaderboardResult.InvalidParameters err -> ResponseEntity.badRequest().body(err);
        };
    }

    @PostMapping("/scores")
    public ResponseEntity<?> submitScore(@Valid @RequestBody SubmitScoreRequest request) {
        SubmitScoreCommand command = new SubmitScoreCommand(
            request.getSessionId(),
            request.getSlotNumber(),
            request.getDeviceId(),
            request.getMachineId(),
            request.getIpId(),
            request.getModeId(),
            request.getScore(),
            request.getAchievedAtUtc(),
            request.getUserId()
        );

        SubmitScoreResult result = submitScoreHandler.handle(command);

        return switch (result) {
            case SubmitScoreResult.Success s -> ResponseEntity.status(HttpStatus.CREATED).body(s);
            case SubmitScoreResult.SlotAlreadyOccupied o -> ResponseEntity.status(HttpStatus.CONFLICT).body(o);
            case SubmitScoreResult.SessionNotFound nf -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(nf);
            case SubmitScoreResult.SessionAlreadyEnded ae -> ResponseEntity.status(HttpStatus.CONFLICT).body(ae);
            case SubmitScoreResult.SessionNotStarted ns -> ResponseEntity.status(HttpStatus.CONFLICT).body(ns);
            case SubmitScoreResult.SlotNotFound snf -> ResponseEntity.status(HttpStatus.CONFLICT).body(snf);
            case SubmitScoreResult.DeviceMismatch dm -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(dm);
            case SubmitScoreResult.EmptyScores es -> ResponseEntity.badRequest().body(es);
        };
    }

    @GetMapping("/{ipId}/{modeId}/rank")
    public ResponseEntity<?> getRank(
        @PathVariable UUID ipId,
        @PathVariable UUID modeId,
        @RequestParam long score
    ) {
        GetPlayerRankResult result = getPlayerRankHandler.handle(
            new GetPlayerRankQuery(ipId, modeId, score)
        );

        return switch (result) {
            case GetPlayerRankResult.Success s -> ResponseEntity.ok(s);
            case GetPlayerRankResult.InvalidParameters err -> ResponseEntity.badRequest().body(err);
        };
    }
}

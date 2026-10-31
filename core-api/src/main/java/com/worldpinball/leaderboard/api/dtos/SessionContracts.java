package com.worldpinball.leaderboard.api.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.UUID;

public final class SessionContracts {
    private SessionContracts() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"modeCode", "clientVersion"})
    public record CreateGameSessionRequest(
        UUID deviceId,
        String modeCode,
        String clientVersion
    ) {
        public CreateGameSessionRequest(String modeCode, String clientVersion) {
            this(null, modeCode, clientVersion);
        }
    }

    public record CreateGameSessionResponse(
        UUID sessionId,
        String nonce,
        long createdAtUtc
    ) {}

    public record AddPlayerRequest(
        int slotNumber,
        UUID userId,
        String guestInitials,
        boolean requiresRegisteredUser
    ) {}

    public record AddPlayerResponse(
        UUID sessionId,
        int slotNumber
    ) {}

    public record KickPlayerResponse(
        UUID sessionId,
        int slotNumber
    ) {}

    public record LaunchSessionResponse(
        UUID sessionId
    ) {}

    public record AbortSessionResponse(
        UUID sessionId
    ) {}

    public record RecordBallScoreRequest(
        UUID deviceId,
        int slotNumber,
        int ballNumber,
        long score
    ) {}

    public record RecordBallScoreResponse(
        UUID sessionId,
        int slotNumber,
        int ballNumber,
        long score
    ) {}

    @JsonPropertyOrder({"slotNumber", "score"})
    public record SlotScoreRequest(
        int slotNumber,
        long score
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"deviceId", "scores"})
    public record SubmitScoreRequest(
        UUID deviceId,
        java.util.List<SlotScoreRequest> scores
    ) {}

    public record SubmitScoreResponse(
        java.util.List<UUID> scoreEntryIds
    ) {}
}

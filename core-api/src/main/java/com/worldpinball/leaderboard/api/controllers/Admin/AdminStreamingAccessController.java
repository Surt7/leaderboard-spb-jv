package com.worldpinball.leaderboard.api.controllers.Admin;

import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.createstreamaccessgrant.CreateStreamAccessGrantCommand;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.createstreamaccessgrant.CreateStreamAccessGrantHandler;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.createstreamaccessgrant.CreateStreamAccessGrantRequest;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.createstreamaccessgrant.CreateStreamAccessGrantResult;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.getstreamaccessgrants.GetStreamAccessGrantsHandler;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.getstreamaccessgrants.GetStreamAccessGrantsQuery;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.getstreamaccessgrants.GetStreamAccessGrantsResult;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant.RevokeStreamAccessGrantCommand;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant.RevokeStreamAccessGrantHandler;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant.RevokeStreamAccessGrantRequest;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant.RevokeStreamAccessGrantResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/streaming-access")
@RequiredArgsConstructor
public class AdminStreamingAccessController {

    private final GetStreamAccessGrantsHandler getStreamAccessGrantsHandler;
    private final CreateStreamAccessGrantHandler createStreamAccessGrantHandler;
    private final RevokeStreamAccessGrantHandler revokeStreamAccessGrantHandler;

    @GetMapping("/grants")
    public ResponseEntity<?> getStreamAccessGrants() {
        var result = getStreamAccessGrantsHandler.handle(new GetStreamAccessGrantsQuery());
        return switch (result) {
            case GetStreamAccessGrantsResult.Success s -> ResponseEntity.ok(s.grants());
        };
    }

    @PostMapping("/grants")
    public ResponseEntity<?> createStreamAccessGrant(@Valid @RequestBody CreateStreamAccessGrantRequest request) {
        var command = new CreateStreamAccessGrantCommand(
                request.principalType(),
                request.principalId(),
                request.displayName(),
                request.origin(),
                request.expiresAtUtc(),
                UUID.fromString("00000000-0000-0000-0000-000000000001")
        );
        var result = createStreamAccessGrantHandler.handle(command);
        return switch (result) {
            case CreateStreamAccessGrantResult.Success s ->
                    ResponseEntity.status(HttpStatus.CREATED).body(Map.of("grantId", s.grantId()));
            case CreateStreamAccessGrantResult.AlreadyExists e -> {
                ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "An active grant already exists for this principal.");
                pd.setTitle("Grant Already Exists");
                yield ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
            }
            case CreateStreamAccessGrantResult.InvalidData e -> {
                ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.error());
                pd.setTitle("Validation Error");
                yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
            }
        };
    }

    @PostMapping("/grants/{grantId}/revoke")
    public ResponseEntity<?> revokeStreamAccessGrant(
            @PathVariable UUID grantId,
            @Valid @RequestBody RevokeStreamAccessGrantRequest request) {
        var result = revokeStreamAccessGrantHandler.handle(new RevokeStreamAccessGrantCommand(grantId, request.reason()));
        return switch (result) {
            case RevokeStreamAccessGrantResult.Success s ->
                    ResponseEntity.ok(Map.of("grantId", s.grantId(), "status", "REVOKED"));
            case RevokeStreamAccessGrantResult.NotFound ignored -> {
                ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Grant not found.");
                pd.setTitle("Not Found");
                yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
            }
            case RevokeStreamAccessGrantResult.AlreadyRevoked ignored -> {
                ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Grant is already revoked.");
                pd.setTitle("Already Revoked");
                yield ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
            }
            case RevokeStreamAccessGrantResult.InvalidReason e -> {
                ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.error());
                pd.setTitle("Invalid Reason");
                yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
            }
        };
    }
}

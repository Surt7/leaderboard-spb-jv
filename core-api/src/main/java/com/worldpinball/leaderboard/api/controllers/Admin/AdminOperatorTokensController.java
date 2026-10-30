package com.worldpinball.leaderboard.api.controllers.Admin;

import com.worldpinball.leaderboard.application.features.machines.Admin.createoperatortoken.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/operator-tokens")
@Tag(name = "Admin - Operator Tokens", description = "Operator token creation and governance")
public class AdminOperatorTokensController {

    private final CreateOperatorTokenHandler handler;

    public AdminOperatorTokensController(CreateOperatorTokenHandler handler) {
        this.handler = handler;
    }

    @PostMapping
    @Operation(summary = "Creates a new operator token")
    @ApiResponse(responseCode = "200", description = "Operator token created")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateOperatorTokenRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "admin") String issuedByUser
    ) {
        CreateOperatorTokenCommand command = new CreateOperatorTokenCommand(
                request.name(),
                request.scopes() != null ? request.scopes() : "*",
                request.validityDays(),
                issuedByUser
        );

        CreateOperatorTokenResult result = handler.handle(command);

        return switch (result) {
            case CreateOperatorTokenResult.Success s -> ResponseEntity.ok(new CreateOperatorTokenResponse(
                    s.tokenId(),
                    s.name(),
                    s.rawToken(),
                    s.prefix(),
                    s.scopes(),
                    s.expiresAtUtc()
            ));
            case CreateOperatorTokenResult.InvalidScopes inv -> ResponseEntity.badRequest().body(Map.of(
                    "message", inv.reason()
            ));
        };
    }
}

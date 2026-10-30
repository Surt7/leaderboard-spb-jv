package com.worldpinball.leaderboard.api.controllers.Admin;

import com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.CreateBootstrapTokenHandler;
import com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.CreateBootstrapTokenResult;
import com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.commands.CreateBootstrapTokenCommand;
import com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.dtos.CreateBootstrapTokenRequest;
import com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.dtos.CreateBootstrapTokenResponse;
import com.worldpinball.leaderboard.domain.enums.WorkstationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/workstations")
@Tag(name = "Admin Workstations", description = "Administration of Aegis workstations")
public class AdminWorkstationsController {

    private final CreateBootstrapTokenHandler handler;

    public AdminWorkstationsController(CreateBootstrapTokenHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/bootstrap-tokens")
    @Operation(summary = "Creates a single-use OTP bootstrap token for a new Aegis workstation")
    public ResponseEntity<?> createBootstrapToken(
            @RequestBody(required = false) CreateBootstrapTokenRequest request,
            Principal principal
    ) {
        String issuedByUser = (principal != null && principal.getName() != null && !principal.getName().isBlank())
                ? principal.getName()
                : "admin";

        WorkstationType type = (request != null && request.workstationType() != null)
                ? request.workstationType()
                : WorkstationType.FieldTerminal;
        String notes = request != null ? request.notes() : null;
        int validityHours = (request != null && request.validityHours() != null)
                ? request.validityHours()
                : 2;

        CreateBootstrapTokenCommand command = new CreateBootstrapTokenCommand(
                issuedByUser,
                type,
                notes,
                validityHours
        );

        CreateBootstrapTokenResult result = handler.handle(command);

        return switch (result) {
            case CreateBootstrapTokenResult.Success success -> ResponseEntity.ok(new CreateBootstrapTokenResponse(
                    success.tokenId(),
                    success.rawBootstrapToken(),
                    success.expiresAtUtc(),
                    success.workstationType()
            ));
            case CreateBootstrapTokenResult.Failure failure -> ResponseEntity.badRequest()
                    .body(Map.of("message", failure.reason()));
        };
    }
}

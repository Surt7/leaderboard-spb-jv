package com.worldpinball.leaderboard.api.controllers.Public;

import com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.EnrollWorkstationHandler;
import com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.EnrollWorkstationResult;
import com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.commands.EnrollWorkstationCommand;
import com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.dtos.EnrollWorkstationRequest;
import com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.dtos.EnrollWorkstationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/workstations")
@Tag(name = "Workstations", description = "Public workstation enrollment")
public class WorkstationsController {

    private final EnrollWorkstationHandler handler;

    public WorkstationsController(EnrollWorkstationHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/enroll")
    @Operation(summary = "Enrolls a new Aegis workstation using a single-use bootstrap OTP")
    public ResponseEntity<?> enroll(@Valid @RequestBody EnrollWorkstationRequest request) {
        EnrollWorkstationCommand command = new EnrollWorkstationCommand(
                request.plainBootstrapToken(),
                request.workstationName(),
                request.publicKeyPem()
        );

        EnrollWorkstationResult result = handler.handle(command);

        return switch (result) {
            case EnrollWorkstationResult.Success success -> ResponseEntity.ok(new EnrollWorkstationResponse(
                    success.certPem(),
                    success.serialNumber(),
                    success.thumbprintSha256(),
                    success.notAfterUtc()
            ));
            case EnrollWorkstationResult.InvalidToken invalidToken -> ResponseEntity.badRequest()
                    .body(Map.of("message", invalidToken.reason()));
            case EnrollWorkstationResult.InvalidPublicKey invalidKey -> ResponseEntity.badRequest()
                    .body(Map.of("message", invalidKey.reason()));
        };
    }
}

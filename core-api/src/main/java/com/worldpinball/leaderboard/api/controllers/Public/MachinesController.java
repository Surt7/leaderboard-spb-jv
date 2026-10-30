package com.worldpinball.leaderboard.api.controllers.Public;

import com.worldpinball.leaderboard.application.features.machines.Public.enroll.EnrollDeviceCommand;
import com.worldpinball.leaderboard.application.features.machines.Public.enroll.EnrollDeviceHandler;
import com.worldpinball.leaderboard.application.features.machines.Public.enroll.EnrollDeviceRequest;
import com.worldpinball.leaderboard.application.features.machines.Public.enroll.EnrollDeviceResponse;
import com.worldpinball.leaderboard.application.features.machines.Public.enroll.EnrollDeviceResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/machines")
@RequiredArgsConstructor
public class MachinesController {

    private final EnrollDeviceHandler enrollHandler;

    @PostMapping("/enroll")
    public ResponseEntity<?> enroll(@Valid @RequestBody EnrollDeviceRequest request) {
        var command = new EnrollDeviceCommand(
                request.machineId(),
                request.plainEnrollmentToken(),
                request.publicKeyPem(),
                request.deviceSerialNumber()
        );

        var result = enrollHandler.handle(command);
        return switch (result) {
            case EnrollDeviceResult.Success s -> ResponseEntity.ok(new EnrollDeviceResponse(
                    s.deviceId(),
                    s.machineId(),
                    s.certPem(),
                    s.certSerialNumber()
            ));
            case EnrollDeviceResult.InvalidToken invalid -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid or expired enrollment token: " + invalid.reason()));
            case EnrollDeviceResult.DeviceAlreadyEnrolled enrolled -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Device with serial '" + enrolled.serialPhysical() + "' is already enrolled."));
            case EnrollDeviceResult.MachineNotFound nf -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Machine with ID '" + nf.machineId() + "' could not be found."));
        };
    }
}

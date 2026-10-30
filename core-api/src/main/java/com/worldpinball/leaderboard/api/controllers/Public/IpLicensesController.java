package com.worldpinball.leaderboard.api.controllers.Public;

import com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses.GetAllIpLicensesHandler;
import com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses.GetAllIpLicensesQuery;
import com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses.GetAllIpLicensesResult;
import com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses.IpLicenseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ip-licenses")
@Tag(name = "IpLicenses", description = "Available intellectual property licenses")
public class IpLicensesController {

    private final GetAllIpLicensesHandler handler;

    public IpLicensesController(GetAllIpLicensesHandler handler) {
        this.handler = handler;
    }

    @GetMapping
    @Operation(summary = "Returns all available IP licenses")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of active IP licenses",
                    content = @Content(schema = @Schema(implementation = IpLicenseDto.class))),
            @ApiResponse(responseCode = "404", description = "No IP licenses found")
    })
    public ResponseEntity<?> getAll() {
        GetAllIpLicensesResult result = handler.handle(new GetAllIpLicensesQuery());

        return switch (result) {
            case GetAllIpLicensesResult.Success s -> ResponseEntity.ok(s.ipLicenses());
            case GetAllIpLicensesResult.NotFound ignored -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No IP licenses found."));
        };
    }
}

package com.worldpinball.leaderboard.api.controllers.Admin;

import com.worldpinball.leaderboard.api.security.permissions.Permission;
import com.worldpinball.leaderboard.api.security.permissions.RequirePermission;
import com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.IssueSoftwareCertificateHandler;
import com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.IssueSoftwareCertificateResult;
import com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.commands.IssueSoftwareCertificateCommand;
import com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.dtos.IssueSoftwareCertificateRequest;
import com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.dtos.IssueSoftwareCertificateResponse;
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
@RequirePermission(Permission.CertificatesIssueSoftware)
@RequestMapping("/api/v1/admin/certificates")
@Tag(name = "Admin Certificates", description = "Administration of software certificates")
public class AdminCertificatesController {

    private final IssueSoftwareCertificateHandler handler;

    public AdminCertificatesController(IssueSoftwareCertificateHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/software")
    @Operation(summary = "Issues an X.509 code signing certificate for software updates")
    public ResponseEntity<?> issueSoftwareCertificate(@Valid @RequestBody IssueSoftwareCertificateRequest request) {
        IssueSoftwareCertificateCommand command = new IssueSoftwareCertificateCommand(
                request.publisherName(),
                request.publicKeyPem()
        );

        IssueSoftwareCertificateResult result = handler.handle(command);

        return switch (result) {
            case IssueSoftwareCertificateResult.Success success -> ResponseEntity.ok(new IssueSoftwareCertificateResponse(
                    success.certPem(),
                    success.serialNumber(),
                    success.thumbprintSha256(),
                    success.notAfterUtc()
            ));
            case IssueSoftwareCertificateResult.InvalidPublicKey invalid -> ResponseEntity.badRequest()
                    .body(Map.of("message", invalid.reason()));
        };
    }
}

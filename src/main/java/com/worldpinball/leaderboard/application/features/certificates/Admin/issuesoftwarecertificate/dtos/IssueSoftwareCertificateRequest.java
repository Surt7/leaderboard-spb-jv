package com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IssueSoftwareCertificateRequest(
        @NotBlank(message = "PublisherName is required.")
        @Size(max = 100, message = "PublisherName must not exceed 100 characters.")
        String publisherName,
        @NotBlank(message = "PublicKeyPem is required.")
        String publicKeyPem
) {}

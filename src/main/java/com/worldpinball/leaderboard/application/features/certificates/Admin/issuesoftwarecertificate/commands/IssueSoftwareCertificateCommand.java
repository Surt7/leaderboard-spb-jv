package com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.commands;

public record IssueSoftwareCertificateCommand(
        String publisherName,
        String publicKeyPem
) {}

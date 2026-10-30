package com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate;

import com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.commands.IssueSoftwareCertificateCommand;

public interface IssueSoftwareCertificateHandler {
    IssueSoftwareCertificateResult handle(IssueSoftwareCertificateCommand command);
}

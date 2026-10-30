package com.worldpinball.leaderboard.application.features.certificates.Admin;

import com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.IssueSoftwareCertificateHandlerImpl;
import com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.IssueSoftwareCertificateResult;
import com.worldpinball.leaderboard.application.features.certificates.Admin.issuesoftwarecertificate.commands.IssueSoftwareCertificateCommand;
import com.worldpinball.leaderboard.application.security.ca.CertificateAuthorityService;
import com.worldpinball.leaderboard.application.security.ca.IssuedSoftwareCertificate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IssueSoftwareCertificateHandler Tests")
class IssueSoftwareCertificateHandlerTest {

    @Mock
    private CertificateAuthorityService caService;

    private IssueSoftwareCertificateHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new IssueSoftwareCertificateHandlerImpl(caService);
    }

    @Test
    @DisplayName("Returns InvalidPublicKey when publisherName is blank")
    void handle_whenPublisherNameIsBlank_shouldReturnInvalidPublicKey() {
        var result = sut.handle(new IssueSoftwareCertificateCommand("  ", "-----BEGIN PUBLIC KEY-----"));
        assertThat(result).isInstanceOf(IssueSoftwareCertificateResult.InvalidPublicKey.class);
    }

    @Test
    @DisplayName("Returns Success when CA service issues software signing certificate")
    void handle_whenValid_shouldReturnSuccess() {
        Instant notBefore = Instant.now();
        Instant notAfter = notBefore.plusSeconds(86400);
        IssuedSoftwareCertificate issued = new IssuedSoftwareCertificate(
                "-----BEGIN CERTIFICATE-----",
                "SERIAL-123",
                "WorldPinball Release Team",
                "THUMB-256",
                "CN=WorldPinball Release Team",
                "CN=WorldPinball Intermediate CA",
                notBefore,
                notAfter
        );
        when(caService.issueSoftwareSigningCertificate("WorldPinball Release Team", "PEM-KEY"))
                .thenReturn(issued);

        var result = sut.handle(new IssueSoftwareCertificateCommand("WorldPinball Release Team", "PEM-KEY"));

        assertThat(result).isInstanceOf(IssueSoftwareCertificateResult.Success.class);
        var success = (IssueSoftwareCertificateResult.Success) result;
        assertThat(success.serialNumber()).isEqualTo("SERIAL-123");
        assertThat(success.thumbprintSha256()).isEqualTo("THUMB-256");
    }
}

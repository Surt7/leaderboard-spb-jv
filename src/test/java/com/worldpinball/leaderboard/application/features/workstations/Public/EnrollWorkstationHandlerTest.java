package com.worldpinball.leaderboard.application.features.workstations.Public;

import com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.EnrollWorkstationHandlerImpl;
import com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.EnrollWorkstationResult;
import com.worldpinball.leaderboard.application.features.workstations.Public.enrollworkstation.commands.EnrollWorkstationCommand;
import com.worldpinball.leaderboard.application.security.ca.CertificateAuthorityService;
import com.worldpinball.leaderboard.application.security.ca.IssuedWorkstationCertificate;
import com.worldpinball.leaderboard.application.security.secrets.SecretService;
import com.worldpinball.leaderboard.domain.entities.EnrollmentToken;
import com.worldpinball.leaderboard.domain.enums.EnrollmentTokenType;
import com.worldpinball.leaderboard.domain.enums.WorkstationType;
import com.worldpinball.leaderboard.infrastructure.EnrollmentTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollWorkstationHandler Tests")
class EnrollWorkstationHandlerTest {

    @Mock
    private EnrollmentTokenRepository enrollmentTokenRepository;

    @Mock
    private CertificateAuthorityService caService;

    private final SecretService secretService = new SecretService();

    private EnrollWorkstationHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new EnrollWorkstationHandlerImpl(enrollmentTokenRepository, caService, secretService);
    }

    @Test
    @DisplayName("Enrolls workstation when bootstrap token is valid")
    void handle_whenTokenValid_shouldConsumeTokenAndIssueCertificate() {
        var generated = secretService.generate();
        Instant now = Instant.now();

        EnrollmentToken token = new EnrollmentToken(
                generated.hashHex(),
                EnrollmentTokenType.AdminBootstrap,
                "admin-1",
                now.minusSeconds(60),
                now.plusSeconds(3600),
                null,
                null,
                1,
                "Admin PC",
                null
        );

        when(enrollmentTokenRepository.findByTokenHash(generated.hashHex())).thenReturn(Optional.of(token));
        IssuedWorkstationCertificate cert = new IssuedWorkstationCertificate(
                "-----BEGIN CERTIFICATE-----",
                "WS-SERIAL-1",
                "AEGIS-PC-01",
                "WS-THUMB-1",
                "CN=AEGIS-PC-01",
                "CN=WorldPinball Intermediate CA",
                now,
                now.plusSeconds(86400)
        );
        when(caService.issueWorkstationCertificate("AEGIS-PC-01", "PUBLIC-KEY-PEM", WorkstationType.AdminWorkstation))
                .thenReturn(cert);

        EnrollWorkstationCommand cmd = new EnrollWorkstationCommand(
                generated.plainBase64(),
                "AEGIS-PC-01",
                "PUBLIC-KEY-PEM"
        );

        EnrollWorkstationResult result = sut.handle(cmd);

        assertThat(result).isInstanceOf(EnrollWorkstationResult.Success.class);
        var success = (EnrollWorkstationResult.Success) result;
        assertThat(success.serialNumber()).isEqualTo("WS-SERIAL-1");
        assertThat(token.getUsesCount()).isEqualTo(1);
        verify(enrollmentTokenRepository).save(token);
    }
}

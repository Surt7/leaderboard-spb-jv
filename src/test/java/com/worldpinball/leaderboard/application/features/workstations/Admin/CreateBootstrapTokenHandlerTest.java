package com.worldpinball.leaderboard.application.features.workstations.Admin;

import com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.CreateBootstrapTokenHandlerImpl;
import com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.CreateBootstrapTokenResult;
import com.worldpinball.leaderboard.application.features.workstations.Admin.createbootstraptoken.commands.CreateBootstrapTokenCommand;
import com.worldpinball.leaderboard.application.security.secrets.SecretService;
import com.worldpinball.leaderboard.domain.entities.EnrollmentToken;
import com.worldpinball.leaderboard.domain.enums.WorkstationType;
import com.worldpinball.leaderboard.infrastructure.EnrollmentTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateBootstrapTokenHandler Tests")
class CreateBootstrapTokenHandlerTest {

    @Mock
    private EnrollmentTokenRepository enrollmentTokenRepository;

    private final SecretService secretService = new SecretService();

    private CreateBootstrapTokenHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new CreateBootstrapTokenHandlerImpl(enrollmentTokenRepository, secretService);
    }

    @Test
    @DisplayName("Creates bootstrap token and persists EnrollmentToken")
    void handle_whenValid_shouldCreateAndPersistBootstrapToken() {
        CreateBootstrapTokenCommand cmd = new CreateBootstrapTokenCommand(
                "admin-user",
                WorkstationType.AdminWorkstation,
                "Test workstation",
                4
        );

        CreateBootstrapTokenResult result = sut.handle(cmd);

        assertThat(result).isInstanceOf(CreateBootstrapTokenResult.Success.class);
        var success = (CreateBootstrapTokenResult.Success) result;
        assertThat(success.rawBootstrapToken()).isNotBlank();
        assertThat(success.workstationType()).isEqualTo(WorkstationType.AdminWorkstation);
        verify(enrollmentTokenRepository).save(any(EnrollmentToken.class));
    }
}

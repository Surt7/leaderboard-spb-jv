package com.worldpinball.leaderboard.application.features.machines.Admin;

import com.worldpinball.leaderboard.application.features.machines.Admin.createoperatortoken.CreateOperatorTokenCommand;
import com.worldpinball.leaderboard.application.features.machines.Admin.createoperatortoken.CreateOperatorTokenHandlerImpl;
import com.worldpinball.leaderboard.application.features.machines.Admin.createoperatortoken.CreateOperatorTokenResult;
import com.worldpinball.leaderboard.domain.entities.OperatorToken;
import com.worldpinball.leaderboard.infrastructure.OperatorTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOperatorTokenHandler Tests")
class CreateOperatorTokenHandlerTest {

    @Mock
    private OperatorTokenRepository repository;

    private Clock clock;
    private CreateOperatorTokenHandlerImpl sut;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"));
        sut = new CreateOperatorTokenHandlerImpl(repository, clock);
    }

    @Test
    @DisplayName("Creates operator token with valid command and saves to repository")
    void handle_whenValidCommand_shouldCreateTokenAndReturnSuccess() {
        CreateOperatorTokenCommand command = new CreateOperatorTokenCommand(
                "USB Technician Key",
                "enroll",
                30,
                "admin-456"
        );

        CreateOperatorTokenResult result = sut.handle(command);

        assertThat(result).isInstanceOf(CreateOperatorTokenResult.Success.class);
        CreateOperatorTokenResult.Success success = (CreateOperatorTokenResult.Success) result;
        assertThat(success.name()).isEqualTo("USB Technician Key");
        assertThat(success.scopes()).isEqualTo("enroll");
        assertThat(success.rawToken()).startsWith("wpt_live_");

        ArgumentCaptor<OperatorToken> captor = ArgumentCaptor.forClass(OperatorToken.class);
        verify(repository).save(captor.capture());

        OperatorToken saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("USB Technician Key");
        assertThat(saved.getScopes()).isEqualTo("enroll");
        assertThat(saved.getIssuedByUserId()).isEqualTo("admin-456");
        assertThat(saved.getExpiresAtUtc()).isNotNull();
    }
}

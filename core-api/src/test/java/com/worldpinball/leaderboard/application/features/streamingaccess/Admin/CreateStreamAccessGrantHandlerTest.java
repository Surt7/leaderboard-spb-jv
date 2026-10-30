package com.worldpinball.leaderboard.application.features.streamingaccess.Admin;

import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.createstreamaccessgrant.CreateStreamAccessGrantCommand;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.createstreamaccessgrant.CreateStreamAccessGrantHandlerImpl;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.createstreamaccessgrant.CreateStreamAccessGrantResult;
import com.worldpinball.leaderboard.domain.entities.StreamAccessGrant;
import com.worldpinball.leaderboard.domain.enums.GrantOrigin;
import com.worldpinball.leaderboard.domain.enums.GrantStatus;
import com.worldpinball.leaderboard.domain.enums.PrincipalType;
import com.worldpinball.leaderboard.infrastructure.StreamAccessGrantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateStreamAccessGrantHandler Tests")
class CreateStreamAccessGrantHandlerTest {

    @Mock
    private StreamAccessGrantRepository repository;

    private Clock clock;
    private CreateStreamAccessGrantHandlerImpl sut;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
        sut = new CreateStreamAccessGrantHandlerImpl(repository, clock);
    }

    @Test
    @DisplayName("Creates and persists stream access grant when command is valid")
    void handle_whenValidCommand_shouldCreateAndPersistGrant() {
        UUID principalId = UUID.randomUUID();
        CreateStreamAccessGrantCommand command = new CreateStreamAccessGrantCommand(
                PrincipalType.ORGANIZATION,
                principalId,
                " Stern Pinball ",
                GrantOrigin.WHITELIST,
                clock.instant().plusSeconds(86400 * 365),
                UUID.randomUUID()
        );

        when(repository.findByPrincipalTypeAndPrincipalIdAndStatus(command.principalType(), command.principalId(), GrantStatus.ACTIVE))
                .thenReturn(Optional.empty());

        CreateStreamAccessGrantResult result = sut.handle(command);

        assertThat(result).isInstanceOf(CreateStreamAccessGrantResult.Success.class);

        ArgumentCaptor<StreamAccessGrant> captor = ArgumentCaptor.forClass(StreamAccessGrant.class);
        verify(repository).save(captor.capture());

        StreamAccessGrant saved = captor.getValue();
        assertThat(saved.getPrincipalId()).isEqualTo(principalId);
        assertThat(saved.getDisplayName()).isEqualTo("Stern Pinball");
        assertThat(saved.getStatus()).isEqualTo(GrantStatus.ACTIVE);
    }

    @Test
    @DisplayName("Returns InvalidData when principalId is null")
    void handle_whenPrincipalIdNull_shouldReturnInvalidData() {
        CreateStreamAccessGrantCommand command = new CreateStreamAccessGrantCommand(
                PrincipalType.USER,
                null,
                "Valid Name",
                GrantOrigin.WHITELIST,
                null,
                null
        );

        CreateStreamAccessGrantResult result = sut.handle(command);

        assertThat(result).isInstanceOf(CreateStreamAccessGrantResult.InvalidData.class);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Returns InvalidData when displayName is null, empty or whitespace")
    void handle_whenDisplayNameIsNullOrWhitespace_shouldReturnInvalidData(String name) {
        CreateStreamAccessGrantCommand command = new CreateStreamAccessGrantCommand(
                PrincipalType.USER,
                UUID.randomUUID(),
                name,
                GrantOrigin.WHITELIST,
                null,
                null
        );

        CreateStreamAccessGrantResult result = sut.handle(command);

        assertThat(result).isInstanceOf(CreateStreamAccessGrantResult.InvalidData.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Returns AlreadyExists when an active grant already exists for principal")
    void handle_whenActiveGrantAlreadyExists_shouldReturnAlreadyExists() {
        UUID principalId = UUID.randomUUID();
        StreamAccessGrant existingGrant = new StreamAccessGrant(
                PrincipalType.ORGANIZATION,
                principalId,
                "Existing Partner",
                GrantOrigin.WHITELIST,
                clock.instant(),
                null,
                null
        );

        CreateStreamAccessGrantCommand command = new CreateStreamAccessGrantCommand(
                PrincipalType.ORGANIZATION,
                principalId,
                "Duplicate Partner",
                GrantOrigin.WHITELIST,
                null,
                null
        );

        when(repository.findByPrincipalTypeAndPrincipalIdAndStatus(command.principalType(), command.principalId(), GrantStatus.ACTIVE))
                .thenReturn(Optional.of(existingGrant));

        CreateStreamAccessGrantResult result = sut.handle(command);

        assertThat(result).isInstanceOf(CreateStreamAccessGrantResult.AlreadyExists.class);
        CreateStreamAccessGrantResult.AlreadyExists alreadyExists = (CreateStreamAccessGrantResult.AlreadyExists) result;
        assertThat(alreadyExists.grantId()).isEqualTo(existingGrant.getGrantId());

        verify(repository, never()).save(any());
    }
}

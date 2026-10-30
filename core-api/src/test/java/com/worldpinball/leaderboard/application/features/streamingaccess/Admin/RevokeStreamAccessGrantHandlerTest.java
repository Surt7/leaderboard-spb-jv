package com.worldpinball.leaderboard.application.features.streamingaccess.Admin;

import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant.RevokeStreamAccessGrantCommand;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant.RevokeStreamAccessGrantHandlerImpl;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.revokestreamaccessgrant.RevokeStreamAccessGrantResult;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RevokeStreamAccessGrantHandler Tests")
class RevokeStreamAccessGrantHandlerTest {

    @Mock
    private StreamAccessGrantRepository repository;

    private RevokeStreamAccessGrantHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new RevokeStreamAccessGrantHandlerImpl(repository);
    }

    @Test
    @DisplayName("Revokes and saves grant when command is valid")
    void handle_whenValidCommand_shouldRevokeAndPersist() {
        StreamAccessGrant grant = new StreamAccessGrant(
                PrincipalType.USER,
                UUID.randomUUID(),
                "StreamerName",
                GrantOrigin.BY_APPROVAL,
                Instant.now().minusSeconds(86400 * 5),
                null,
                null
        );

        when(repository.findById(grant.getGrantId())).thenReturn(Optional.of(grant));

        String reason = "Repeated copyright infringement.";
        RevokeStreamAccessGrantCommand command = new RevokeStreamAccessGrantCommand(grant.getGrantId(), reason);

        RevokeStreamAccessGrantResult result = sut.handle(command);

        assertThat(result).isInstanceOf(RevokeStreamAccessGrantResult.Success.class);
        RevokeStreamAccessGrantResult.Success success = (RevokeStreamAccessGrantResult.Success) result;
        assertThat(success.grantId()).isEqualTo(grant.getGrantId());

        assertThat(grant.getStatus()).isEqualTo(GrantStatus.REVOKED);
        assertThat(grant.getRevocationReason()).isEqualTo(reason);
        verify(repository).save(grant);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Returns InvalidReason when reason is null, empty or whitespace")
    void handle_whenReasonIsNullOrWhitespace_shouldReturnInvalidReason(String invalidReason) {
        RevokeStreamAccessGrantCommand command = new RevokeStreamAccessGrantCommand(UUID.randomUUID(), invalidReason);

        RevokeStreamAccessGrantResult result = sut.handle(command);

        assertThat(result).isInstanceOf(RevokeStreamAccessGrantResult.InvalidReason.class);
        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Returns NotFound when grant does not exist")
    void handle_whenGrantNotFound_shouldReturnNotFound() {
        UUID grantId = UUID.randomUUID();
        when(repository.findById(grantId)).thenReturn(Optional.empty());

        RevokeStreamAccessGrantCommand command = new RevokeStreamAccessGrantCommand(grantId, "Valid reason");

        RevokeStreamAccessGrantResult result = sut.handle(command);

        assertThat(result).isInstanceOf(RevokeStreamAccessGrantResult.NotFound.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Returns AlreadyRevoked when grant is already revoked")
    void handle_whenGrantAlreadyRevoked_shouldReturnAlreadyRevoked() {
        StreamAccessGrant grant = new StreamAccessGrant(
                PrincipalType.ORGANIZATION,
                UUID.randomUUID(),
                "Old Partner",
                GrantOrigin.WHITELIST,
                Instant.now().minusSeconds(86400 * 10),
                null,
                null
        );
        grant.revoke("Previous reason", Instant.now().minusSeconds(86400 * 2));

        when(repository.findById(grant.getGrantId())).thenReturn(Optional.of(grant));

        RevokeStreamAccessGrantCommand command = new RevokeStreamAccessGrantCommand(grant.getGrantId(), "New reason");

        RevokeStreamAccessGrantResult result = sut.handle(command);

        assertThat(result).isInstanceOf(RevokeStreamAccessGrantResult.AlreadyRevoked.class);
        verify(repository, never()).save(any());
    }
}

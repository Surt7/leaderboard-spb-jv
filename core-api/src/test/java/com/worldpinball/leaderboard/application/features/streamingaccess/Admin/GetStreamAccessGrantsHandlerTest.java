package com.worldpinball.leaderboard.application.features.streamingaccess.Admin;

import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.getstreamaccessgrants.GetStreamAccessGrantsHandlerImpl;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.getstreamaccessgrants.GetStreamAccessGrantsQuery;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.getstreamaccessgrants.GetStreamAccessGrantsResult;
import com.worldpinball.leaderboard.application.features.streamingaccess.Admin.getstreamaccessgrants.StreamAccessGrantDto;
import com.worldpinball.leaderboard.domain.entities.StreamAccessGrant;
import com.worldpinball.leaderboard.domain.enums.GrantOrigin;
import com.worldpinball.leaderboard.domain.enums.GrantStatus;
import com.worldpinball.leaderboard.domain.enums.PrincipalType;
import com.worldpinball.leaderboard.infrastructure.StreamAccessGrantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetStreamAccessGrantsHandler Tests")
class GetStreamAccessGrantsHandlerTest {

    @Mock
    private StreamAccessGrantRepository repository;

    private GetStreamAccessGrantsHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new GetStreamAccessGrantsHandlerImpl(repository);
    }

    @Test
    @DisplayName("Maps and returns success with grants when grants exist")
    void handle_whenGrantsExist_shouldMapAndReturnSuccess() {
        StreamAccessGrant grant1 = new StreamAccessGrant(
                PrincipalType.ORGANIZATION,
                UUID.randomUUID(),
                "Stern Pro Circuit",
                GrantOrigin.WHITELIST,
                Instant.now(),
                null,
                null
        );

        StreamAccessGrant grant2 = new StreamAccessGrant(
                PrincipalType.USER,
                UUID.randomUUID(),
                "StreamerPro",
                GrantOrigin.BY_APPROVAL,
                Instant.now(),
                null,
                null
        );

        when(repository.findAllByOrderByCreatedAtUtcDesc()).thenReturn(List.of(grant1, grant2));

        GetStreamAccessGrantsResult result = sut.handle(new GetStreamAccessGrantsQuery());

        assertThat(result).isInstanceOf(GetStreamAccessGrantsResult.Success.class);
        GetStreamAccessGrantsResult.Success success = (GetStreamAccessGrantsResult.Success) result;
        assertThat(success.grants()).hasSize(2);

        StreamAccessGrantDto dto1 = success.grants().stream()
                .filter(g -> g.grantId().equals(grant1.getGrantId()))
                .findFirst()
                .orElseThrow();

        assertThat(dto1.displayName()).isEqualTo("Stern Pro Circuit");
        assertThat(dto1.principalType()).isEqualTo(PrincipalType.ORGANIZATION);
        assertThat(dto1.origin()).isEqualTo(GrantOrigin.WHITELIST);
        assertThat(dto1.status()).isEqualTo(GrantStatus.ACTIVE);
    }

    @Test
    @DisplayName("Returns success with empty list when no grants exist")
    void handle_whenNoGrantsExist_shouldReturnEmptyList() {
        when(repository.findAllByOrderByCreatedAtUtcDesc()).thenReturn(List.of());

        GetStreamAccessGrantsResult result = sut.handle(new GetStreamAccessGrantsQuery());

        assertThat(result).isInstanceOf(GetStreamAccessGrantsResult.Success.class);
        GetStreamAccessGrantsResult.Success success = (GetStreamAccessGrantsResult.Success) result;
        assertThat(success.grants()).isEmpty();
    }
}

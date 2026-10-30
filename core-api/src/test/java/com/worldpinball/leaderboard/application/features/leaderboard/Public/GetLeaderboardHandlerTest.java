package com.worldpinball.leaderboard.application.features.leaderboard.Public;

import com.worldpinball.leaderboard.application.features.leaderboard.Public.dtos.LeaderboardRowDto;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getleaderboard.GetLeaderboardHandlerImpl;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getleaderboard.GetLeaderboardQuery;
import com.worldpinball.leaderboard.application.features.leaderboard.Public.getleaderboard.GetLeaderboardResult;
import com.worldpinball.leaderboard.application.services.RealtimeLeaderboardService;
import com.worldpinball.leaderboard.domain.entities.GameMode;
import com.worldpinball.leaderboard.domain.entities.IpLicense;
import com.worldpinball.leaderboard.domain.entities.ScoreEntry;
import com.worldpinball.leaderboard.infrastructure.GameModeRepository;
import com.worldpinball.leaderboard.infrastructure.IpLicenseRepository;
import com.worldpinball.leaderboard.infrastructure.ScoreEntryRepository;
import com.worldpinball.leaderboard.infrastructure.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetLeaderboardHandler Tests")
class GetLeaderboardHandlerTest {

    @Mock
    private ScoreEntryRepository scoreEntryRepository;

    @Mock
    private IpLicenseRepository ipLicenseRepository;

    @Mock
    private GameModeRepository gameModeRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private RealtimeLeaderboardService realtimeLeaderboardService;

    private GetLeaderboardHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new GetLeaderboardHandlerImpl(
                scoreEntryRepository,
                ipLicenseRepository,
                gameModeRepository,
                userProfileRepository,
                realtimeLeaderboardService
        );
    }

    @Test
    @DisplayName("When cache has scores, returns from cache without querying database")
    void handle_whenCacheHasScores_shouldReturnFromCacheWithoutQueryingDb() {
        UUID ipId = UUID.randomUUID();
        UUID modeId = UUID.randomUUID();
        GetLeaderboardQuery query = new GetLeaderboardQuery(ipId, modeId, null, null, 10);

        List<LeaderboardRowDto> cachedRows = List.of(
                new LeaderboardRowDto(1L, UUID.randomUUID(), "TOM", "Thomas", 150_000_000L, Instant.now(), "Star Wars", "Classic"),
                new LeaderboardRowDto(2L, UUID.randomUUID(), "BOB", "Bobby", 90_000_000L, Instant.now(), "Star Wars", "Classic")
        );

        when(realtimeLeaderboardService.getCachedLeaderboard(ipId, modeId, 10))
                .thenReturn(Optional.of(cachedRows));

        GetLeaderboardResult result = sut.handle(query);

        assertThat(result).isInstanceOf(GetLeaderboardResult.Success.class);
        GetLeaderboardResult.Success success = (GetLeaderboardResult.Success) result;
        assertThat(success.rows()).hasSize(2);

        verifyNoInteractions(scoreEntryRepository);
        verifyNoInteractions(ipLicenseRepository);
        verifyNoInteractions(gameModeRepository);
    }

    @Test
    @DisplayName("When cache miss, queries database and warms up cache")
    void handle_whenCacheMiss_shouldQueryDatabaseAndWarmupCache() {
        UUID ipId = UUID.randomUUID();
        UUID modeId = UUID.randomUUID();
        GetLeaderboardQuery query = new GetLeaderboardQuery(ipId, modeId, null, null, 10);

        when(realtimeLeaderboardService.getCachedLeaderboard(ipId, modeId, 10))
                .thenReturn(Optional.empty());

        IpLicense ipLicense = new IpLicense("Star Wars", "star-wars", true, ipId);
        GameMode gameMode = new GameMode("CLASSIC", "Classic", "Standard", true, modeId);

        when(ipLicenseRepository.findById(ipId)).thenReturn(Optional.of(ipLicense));
        when(gameModeRepository.findById(modeId)).thenReturn(Optional.of(gameMode));

        ScoreEntry scoreEntry = new ScoreEntry(
                UUID.randomUUID(),
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                ipId,
                modeId,
                150_000_000L,
                Instant.now(),
                UUID.randomUUID()
        );

        when(scoreEntryRepository.findTopScoresByIpAndMode(ipId, modeId, PageRequest.of(0, 10)))
                .thenReturn(List.of(scoreEntry));

        GetLeaderboardResult result = sut.handle(query);

        assertThat(result).isInstanceOf(GetLeaderboardResult.Success.class);
        GetLeaderboardResult.Success success = (GetLeaderboardResult.Success) result;
        assertThat(success.rows()).hasSize(1);
        assertThat(success.rows().get(0).score()).isEqualTo(150_000_000L);

        verify(realtimeLeaderboardService).setCachedLeaderboard(
                eq(ipId), eq(modeId), eq(10), any(), eq(Duration.ofMinutes(1))
        );
    }

    @Test
    @DisplayName("When neither IDs nor slugs provided, returns InvalidParameters")
    void handle_whenMissingParameters_shouldReturnInvalidParameters() {
        GetLeaderboardQuery query = new GetLeaderboardQuery(null, null, null, null, 10);

        GetLeaderboardResult result = sut.handle(query);

        assertThat(result).isInstanceOf(GetLeaderboardResult.InvalidParameters.class);
    }
}

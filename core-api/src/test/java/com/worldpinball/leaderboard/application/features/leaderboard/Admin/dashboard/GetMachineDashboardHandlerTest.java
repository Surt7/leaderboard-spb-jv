package com.worldpinball.leaderboard.application.features.leaderboard.Admin.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMachineDashboardHandler Tests")
class GetMachineDashboardHandlerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JdbcClient jdbcClient;

    private GetMachineDashboardHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new GetMachineDashboardHandlerImpl(jdbcClient);
    }

    @Test
    @DisplayName("Returns Success when machine stats exist")
    void handle_whenMachineExists_shouldReturnSuccess() {
        UUID machineId = UUID.randomUUID();
        MachineDashboardDto stats = new MachineDashboardDto(
                machineId,
                "WP-123",
                10,
                5_000_000L,
                2_500_000L,
                4,
                Instant.now()
        );

        when(jdbcClient.sql(anyString())
                .param(anyString(), any())
                .query(any(RowMapper.class))
                .optional()
        ).thenReturn(Optional.of(stats));

        GetMachineDashboardResult result = sut.handle(new GetMachineDashboardQuery(machineId));

        assertThat(result).isInstanceOf(GetMachineDashboardResult.Success.class);
        GetMachineDashboardResult.Success success = (GetMachineDashboardResult.Success) result;
        assertThat(success.stats()).isEqualTo(stats);
    }

    @Test
    @DisplayName("Returns NotFound when machine stats not found")
    void handle_whenMachineNotFound_shouldReturnNotFound() {
        UUID machineId = UUID.randomUUID();

        when(jdbcClient.sql(anyString())
                .param(anyString(), any())
                .query(any(RowMapper.class))
                .optional()
        ).thenReturn(Optional.empty());

        GetMachineDashboardResult result = sut.handle(new GetMachineDashboardQuery(machineId));

        assertThat(result).isInstanceOf(GetMachineDashboardResult.NotFound.class);
    }

    @Test
    @DisplayName("Returns NotFound when machineId is null")
    void handle_whenMachineIdIsNull_shouldReturnNotFound() {
        GetMachineDashboardResult result = sut.handle(new GetMachineDashboardQuery(null));

        assertThat(result).isInstanceOf(GetMachineDashboardResult.NotFound.class);
        verifyNoInteractions(jdbcClient);
    }
}

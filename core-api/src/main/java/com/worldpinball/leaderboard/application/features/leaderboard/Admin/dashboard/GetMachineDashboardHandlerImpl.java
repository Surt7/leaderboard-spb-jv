package com.worldpinball.leaderboard.application.features.leaderboard.Admin.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class GetMachineDashboardHandlerImpl implements GetMachineDashboardHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetMachineDashboardHandlerImpl.class);

    private static final String SQL = """
        SELECT
            m.machine_id,
            m.worldpinball_serial_number,
            COALESCE(COUNT(DISTINCT gs.session_id), 0) AS total_games_played,
            COALESCE(MAX(se.score), 0) AS high_score,
            COALESCE(AVG(se.score), 0) AS average_score,
            COALESCE(COUNT(DISTINCT se.user_id), 0) AS total_unique_players,
            MAX(gs.started_at_utc) AS last_played_at_utc
        FROM machines m
        LEFT JOIN game_sessions gs ON gs.machine_id = m.machine_id
        LEFT JOIN score_entries se ON se.machine_id = m.machine_id
        WHERE m.machine_id = :machineId
        GROUP BY m.machine_id, m.worldpinball_serial_number
    """;

    private final JdbcClient jdbcClient;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public GetMachineDashboardHandlerImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public GetMachineDashboardResult handle(GetMachineDashboardQuery query) {
        if (query.machineId() == null) {
            logger.warn("[2601:InvalidMachineId] Machine ID cannot be empty.");
            return new GetMachineDashboardResult.NotFound(query.machineId());
        }

        if (entityManager != null) {
            entityManager.flush();
        }

        Optional<MachineDashboardDto> statsOpt = jdbcClient.sql(SQL)
                .param("machineId", query.machineId())
                .query((rs, rowNum) -> {
                    UUID machineId = (UUID) rs.getObject("machine_id");
                    String serialNumber = rs.getString("worldpinball_serial_number");
                    int totalGamesPlayed = rs.getInt("total_games_played");
                    long highScore = rs.getLong("high_score");
                    long averageScore = Math.round(rs.getDouble("average_score"));
                    int totalUniquePlayers = rs.getInt("total_unique_players");
                    Timestamp ts = rs.getTimestamp("last_played_at_utc");
                    Instant lastPlayedAt = ts != null ? ts.toInstant() : null;

                    return new MachineDashboardDto(
                            machineId,
                            serialNumber,
                            totalGamesPlayed,
                            highScore,
                            averageScore,
                            totalUniquePlayers,
                            lastPlayedAt
                    );
                })
                .optional();

        if (statsOpt.isEmpty()) {
            logger.warn("[2600:StatNotFound] Stat on machine: {} could not be found.", query.machineId());
            return new GetMachineDashboardResult.NotFound(query.machineId());
        }

        return new GetMachineDashboardResult.Success(statsOpt.get());
    }
}

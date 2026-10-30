package com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.recordballscore.commands.RecordBallScoreCommand;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.common.EventId;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordBallScoreHandlerImpl implements RecordBallScoreHandler {

    private static final EventId SESSION_NOT_FOUND = new EventId(2920, "SessionNotFound");
    private static final EventId DEVICE_MISMATCH = new EventId(2921, "DeviceMismatch");

    private final GameSessionRepository gameSessionRepository;
    private final LobbyNotificationService lobbyNotifier;
    private final Clock clock;

    @Override
    @Transactional
    public RecordBallScoreResult handle(RecordBallScoreCommand command) {
        Optional<GameSession> sessionOpt = gameSessionRepository.findByIdWithPlayers(command.sessionId());
        if (sessionOpt.isEmpty()) {
            log.warn("{} Session {} not found.", SESSION_NOT_FOUND.tag(), command.sessionId());
            return new RecordBallScoreResult.SessionNotFound(command.sessionId());
        }

        GameSession session = sessionOpt.get();

        if (!Objects.equals(session.getDeviceId(), command.deviceId())) {
            log.warn("{} Device {} mismatch for session {} (expected {}).",
                DEVICE_MISMATCH.tag(), command.deviceId(), command.sessionId(), session.getDeviceId());
            return new RecordBallScoreResult.DeviceMismatch(session.getDeviceId(), command.deviceId());
        }

        Instant nowUtc = Instant.now(clock);

        try {
            session.recordBallScore(command.slotNumber(), command.ballNumber(), command.score(), nowUtc);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            log.warn(ex.getMessage(), ex);
            return new RecordBallScoreResult.CannotRecordBallScore(ex.getMessage());
        }

        gameSessionRepository.save(session);
        lobbyNotifier.notifyScoreSubmitted(session.getSessionId(), command.slotNumber(), command.score());

        return new RecordBallScoreResult.Success(
            session.getSessionId(),
            command.slotNumber(),
            command.ballNumber(),
            command.score()
        );
    }
}

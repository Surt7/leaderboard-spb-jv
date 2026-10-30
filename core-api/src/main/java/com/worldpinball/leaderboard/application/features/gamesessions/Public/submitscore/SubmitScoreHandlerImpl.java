package com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.submitscore.commands.SubmitScoreCommand;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.application.services.RealtimeLeaderboardService;
import com.worldpinball.leaderboard.common.EventId;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.entities.ScoreEntry;
import com.worldpinball.leaderboard.domain.enums.GameSessionState;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import com.worldpinball.leaderboard.infrastructure.ScoreEntryRepository;
import com.worldpinball.leaderboard.infrastructure.outbox.DomainEventOutboxRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmitScoreHandlerImpl implements SubmitScoreHandler {

    private static final EventId SESSION_NOT_FOUND = new EventId(2400, "SessionNotFound");
    private static final EventId SCORE_ALREADY_EXISTS = new EventId(2401, "ScoreAlreadyExists");
    private static final EventId SESSION_ALREADY_ENDED = new EventId(2402, "SessionAlreadyEnded");
    private static final EventId SESSION_NOT_STARTED = new EventId(2403, "SessionNotStarted");
    private static final EventId DEVICE_MISMATCH = new EventId(2404, "DeviceMismatch");
    private static final EventId SLOT_NOT_FOUND = new EventId(2405, "SlotNotFound");
    private static final EventId SCORE_SUBMITTED = new EventId(2406, "ScoreSubmitted");

    private final GameSessionRepository gameSessionRepository;
    private final MachineRepository machineRepository;
    private final ScoreEntryRepository scoreEntryRepository;
    private final DomainEventOutboxRelay domainEventOutboxRelay;
    private final RealtimeLeaderboardService realtimeLeaderboardService;
    private final LobbyNotificationService lobbyNotifier;
    private final Clock clock;

    @Override
    @Transactional
    public SubmitScoreResult handle(SubmitScoreCommand command) {
        if (command.scores() == null || command.scores().isEmpty()) {
            return new SubmitScoreResult.EmptyScores();
        }

        Instant nowUtc = Instant.now(clock);

        GameSession session = null;
        if (gameSessionRepository != null) {
            var sessionOpt = gameSessionRepository.findByIdWithPlayers(command.sessionId());
            if (sessionOpt.isEmpty()) {
                log.warn("{} Session {} could not be found.", SESSION_NOT_FOUND.tag(), command.sessionId());
                return new SubmitScoreResult.SessionNotFound(command.sessionId());
            }
            session = sessionOpt.get();

            if (!session.getDeviceId().equals(command.deviceId())) {
                log.warn("{} Device mismatch: expected {}, actual {}.",
                        DEVICE_MISMATCH.tag(), session.getDeviceId(), command.deviceId());
                return new SubmitScoreResult.DeviceMismatch(session.getDeviceId(), command.deviceId());
            }

            if (session.getState() == GameSessionState.ENDED) {
                log.warn("{} Session {} is already ended.", SESSION_ALREADY_ENDED.tag(), session.getSessionId());
                return new SubmitScoreResult.SessionAlreadyEnded(session.getSessionId());
            }

            if (session.getState() != GameSessionState.STARTED
                    && session.getState() != GameSessionState.IN_GAME
                    && session.getState() != GameSessionState.RESUMED
                    && session.getState() != GameSessionState.PAUSED) {
                log.warn("{} Session {} is not started.", SESSION_NOT_STARTED.tag(), session.getSessionId());
                return new SubmitScoreResult.SessionNotStarted(session.getSessionId());
            }
        }

        UUID machineId = session != null ? session.getMachineId() : UUID.randomUUID();
        UUID modeId = session != null ? session.getModeId() : UUID.randomUUID();
        UUID ipId = UUID.randomUUID();
        if (session != null && machineRepository != null) {
            ipId = machineRepository.findById(session.getMachineId())
                    .map(Machine::getIpId)
                    .orElse(ipId);
        }

        List<ScoreEntry> entriesToSave = new ArrayList<>();
        for (var scoreDto : command.scores()) {
            var existing = scoreEntryRepository.findBySessionIdAndSlotNumber(
                    command.sessionId(),
                    scoreDto.slotNumber()
            );

            if (existing.isPresent()) {
                log.warn("{} A score for session {} and slot {} already exists.",
                        SCORE_ALREADY_EXISTS.tag(), command.sessionId(), scoreDto.slotNumber());
                return new SubmitScoreResult.SlotAlreadyOccupied(scoreDto.slotNumber());
            }

            UUID userId = null;
            if (session != null) {
                var playerOpt = session.getPlayers().stream()
                        .filter(p -> p.getSlotNumber() == scoreDto.slotNumber())
                        .findFirst();
                if (playerOpt.isEmpty()) {
                    log.warn("{} Slot {} not found in session {}.",
                            SLOT_NOT_FOUND.tag(), scoreDto.slotNumber(), session.getSessionId());
                    return new SubmitScoreResult.SlotNotFound(scoreDto.slotNumber());
                }
                userId = playerOpt.get().getUserId();
            }

            ScoreEntry entry = new ScoreEntry(
                    command.sessionId(),
                    scoreDto.slotNumber(),
                    command.deviceId(),
                    machineId,
                    ipId,
                    modeId,
                    scoreDto.score(),
                    nowUtc,
                    userId
            );
            entriesToSave.add(entry);
        }

        long totalScores = scoreEntryRepository.countBySessionId(command.sessionId()) + entriesToSave.size();
        int expectedPlayers = (session != null && !session.getPlayers().isEmpty()) ? session.getPlayers().size() : 1;
        if (session != null && totalScores >= expectedPlayers) {
            session.endSession(nowUtc);
            gameSessionRepository.save(session);
        }

        List<UUID> scoreEntryIds = new ArrayList<>();
        for (ScoreEntry entry : entriesToSave) {
            ScoreEntry saved = scoreEntryRepository.save(entry);
            scoreEntryIds.add(saved.getScoreEntryId());
            domainEventOutboxRelay.persistOutboxMessagesAndNotifyAfterCommit(saved);

            realtimeLeaderboardService.addScore(
                    saved.getIpId(),
                    saved.getModeId(),
                    saved.getScoreEntryId(),
                    saved.getScore()
            );
            realtimeLeaderboardService.invalidateCachedLeaderboard(saved.getIpId(), saved.getModeId());

            lobbyNotifier.notifyScoreSubmitted(saved.getSessionId(), saved.getSlotNumber(), saved.getScore());

            log.info("{} Score {} recorded for session {} slot {}.",
                    SCORE_SUBMITTED.tag(), saved.getScore(), saved.getSessionId(), saved.getSlotNumber());
        }

        if (session != null) {
            lobbyNotifier.notifyLobbyUpdated(session.getSessionId());
        }

        return new SubmitScoreResult.Success(scoreEntryIds);
    }
}

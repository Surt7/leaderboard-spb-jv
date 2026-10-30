package com.worldpinball.leaderboard.application.features.matchmaking.Public.matchnextplayer;

import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.application.services.MatchmakingQueueService;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.domain.enums.GameSessionState;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class MatchNextPlayerHandlerImpl implements MatchNextPlayerHandler {

    private static final Logger logger = LoggerFactory.getLogger(MatchNextPlayerHandlerImpl.class);

    private final GameSessionRepository sessionRepository;
    private final MatchmakingQueueService queueService;
    private final LobbyNotificationService lobbyNotificationService;

    public MatchNextPlayerHandlerImpl(
            GameSessionRepository sessionRepository,
            MatchmakingQueueService queueService,
            LobbyNotificationService lobbyNotificationService
    ) {
        this.sessionRepository = sessionRepository;
        this.queueService = queueService;
        this.lobbyNotificationService = lobbyNotificationService;
    }

    @Override
    @Transactional
    public MatchNextPlayerResult handle(MatchNextPlayerCommand command) {
        Optional<GameSession> sessionOpt = sessionRepository.findById(command.sessionId());
        if (sessionOpt.isEmpty()) {
            logger.warn("[2900:NoMachinesFound] Game session {} could not be found.", command.sessionId());
            return new MatchNextPlayerResult.SessionNotFound(command.sessionId());
        }

        GameSession session = sessionOpt.get();
        if (session.getPlayers().size() >= 4) {
            logger.warn("[2902:SessionIsFull] Game session {} is full.", command.sessionId());
            return new MatchNextPlayerResult.SessionFull(command.sessionId());
        }

        Optional<UUID> dequeuedPlayer = queueService.dequeueNext(command.machineId().toString());
        if (dequeuedPlayer.isEmpty()) {
            logger.warn("[2901:NoPlayerInQueue] Queue from machine {} for session {} has no waiting players.",
                    command.machineId(), command.sessionId());
            return new MatchNextPlayerResult.NoPlayerInQueue();
        }

        UUID playerId = dequeuedPlayer.get();

        int nextSlot = IntStream.rangeClosed(1, 4)
                .filter(slot -> session.getPlayers().stream().noneMatch(p -> p.getSlotNumber() == slot))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No available slot found"));

        session.addPlayer(nextSlot, playerId);

        if (session.getState() == GameSessionState.SEARCHING_PLAYER && session.getPlayers().size() >= 2) {
            session.setMatchFound(Instant.now());
            lobbyNotificationService.notifyMatchFound(session.getSessionId(), session.getMachineId());
        }

        sessionRepository.save(session);

        return new MatchNextPlayerResult.Success(
                playerId,
                nextSlot,
                session.getState() == GameSessionState.MATCH_FOUND
        );
    }
}

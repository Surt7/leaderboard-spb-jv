package com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.abortsession.commands.AbortSessionCommand;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbortSessionHandlerImpl implements AbortSessionHandler {

    private static final EventId SESSION_NOT_FOUND = new EventId(2910, "SessionNotFound");

    private final GameSessionRepository gameSessionRepository;
    private final LobbyNotificationService lobbyNotifier;
    private final Clock clock;

    @Override
    @Transactional
    public AbortSessionResult handle(AbortSessionCommand command) {
        Optional<GameSession> sessionOpt = gameSessionRepository.findByIdWithPlayers(command.sessionId());
        if (sessionOpt.isEmpty()) {
            log.warn("{} Game session {} not found.", SESSION_NOT_FOUND.tag(), command.sessionId());
            return new AbortSessionResult.SessionNotFound(command.sessionId());
        }

        GameSession session = sessionOpt.get();

        try {
            Instant nowUtc = Instant.now(clock);
            session.abort(nowUtc);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return new AbortSessionResult.CannotAbortSession(ex.getMessage());
        }

        gameSessionRepository.save(session);
        lobbyNotifier.notifySessionAborted(session.getSessionId());

        return new AbortSessionResult.Success(session.getSessionId());
    }
}

package com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.launchsession.commands.LaunchSessionCommand;
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
public class LaunchSessionHandlerImpl implements LaunchSessionHandler {

    private static final EventId SESSION_NOT_FOUND = new EventId(2800, "SessionNotFound");

    private final GameSessionRepository gameSessionRepository;
    private final LobbyNotificationService lobbyNotifier;
    private final Clock clock;

    @Override
    @Transactional
    public LaunchSessionResult handle(LaunchSessionCommand command) {
        Optional<GameSession> sessionOpt = gameSessionRepository.findByIdWithPlayers(command.sessionId());
        if (sessionOpt.isEmpty()) {
            log.warn("{} Game session {} not found.", SESSION_NOT_FOUND.tag(), command.sessionId());
            return new LaunchSessionResult.SessionNotFound(command.sessionId());
        }

        GameSession session = sessionOpt.get();

        try {
            Instant nowUtc = Instant.now(clock);
            session.launch(nowUtc);
        } catch (IllegalStateException ex) {
            return new LaunchSessionResult.CannotStartSession(ex.getMessage());
        }

        gameSessionRepository.save(session);
        lobbyNotifier.notifySessionLaunched(session.getSessionId());

        return new LaunchSessionResult.Success(session.getSessionId());
    }
}

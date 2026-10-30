package com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.kickplayer.commands.KickPlayerCommand;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.common.EventId;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.domain.enums.GameSessionState;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KickPlayerHandlerImpl implements KickPlayerHandler {

    private static final EventId SESSION_NOT_FOUND = new EventId(2600, "SessionNotFound");
    private static final EventId INVALID_SLOT = new EventId(2601, "InvalidSlot");
    private static final EventId PLAYER_NOT_FOUND = new EventId(2602, "PlayerNotFound");
    private static final EventId CANNOT_REMOVE_PLAYER_ACTIVE_SESSION = new EventId(2603, "CannotRemovePlayerActiveSession");

    private final GameSessionRepository gameSessionRepository;
    private final LobbyNotificationService lobbyNotifier;

    @Override
    @Transactional
    public KickPlayerResult handle(KickPlayerCommand command) {
        if (command.slotNumber() < 1 || command.slotNumber() > 4) {
            log.warn("{} Slot {} in session {} is invalid.",
                INVALID_SLOT.tag(), command.slotNumber(), command.sessionId());
            return new KickPlayerResult.InvalidSlot(command.slotNumber());
        }

        Optional<GameSession> sessionOpt = gameSessionRepository.findByIdWithPlayers(command.sessionId());
        if (sessionOpt.isEmpty()) {
            log.warn("{} Game session {} could not be found.",
                SESSION_NOT_FOUND.tag(), command.sessionId());
            return new KickPlayerResult.SessionNotFound(command.sessionId());
        }

        GameSession session = sessionOpt.get();

        if (session.getState() != GameSessionState.CREATED
            && session.getState() != GameSessionState.PENDING
            && session.getState() != GameSessionState.SEARCHING_PLAYER
            && session.getState() != GameSessionState.MATCH_FOUND
            && session.getState() != GameSessionState.JOINED) {
            log.warn("{} Cannot kick player from session {} in state {}.",
                CANNOT_REMOVE_PLAYER_ACTIVE_SESSION.tag(), session.getSessionId(), session.getState());
            return new KickPlayerResult.CannotRemovePlayerActiveSession(session.getSessionId());
        }

        boolean playerExists = session.getPlayers().stream().anyMatch(p -> p.getSlotNumber() == command.slotNumber());
        if (!playerExists) {
            log.warn("{} No player found in slot {} for session {}.",
                PLAYER_NOT_FOUND.tag(), command.slotNumber(), session.getSessionId());
            return new KickPlayerResult.PlayerNotFound(command.slotNumber());
        }

        session.removePlayer(command.slotNumber());
        gameSessionRepository.save(session);
        lobbyNotifier.notifyLobbyUpdated(session.getSessionId());

        log.info("Player removed from slot {} in session {}.", command.slotNumber(), session.getSessionId());
        return new KickPlayerResult.Success(session.getSessionId(), command.slotNumber());
    }
}

package com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.addplayer.commands.AddPlayerCommand;
import com.worldpinball.leaderboard.application.services.LobbyNotificationService;
import com.worldpinball.leaderboard.common.EventId;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.domain.enums.GameSessionState;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddPlayerHandlerImpl implements AddPlayerHandler {

    private static final EventId SESSION_NOT_FOUND = new EventId(2500, "SessionNotFound");
    private static final EventId SESSION_NOT_JOINABLE = new EventId(2501, "SessionNotJoinable");
    private static final EventId SLOT_ALREADY_OCCUPIED = new EventId(2502, "SlotAlreadyOccupied");
    private static final EventId REGISTERED_USER_REQUIRED = new EventId(2503, "RegisteredUserRequired");
    private static final EventId INVALID_SLOT = new EventId(2504, "InvalidSlot");
    private static final EventId USER_ALREADY_REGISTERED = new EventId(2505, "UserAlreadyRegistered");

    private final GameSessionRepository gameSessionRepository;
    private final LobbyNotificationService lobbyNotifier;

    @Override
    @Transactional
    public AddPlayerResult handle(AddPlayerCommand command) {
        Optional<GameSession> sessionOpt = gameSessionRepository.findByIdWithPlayers(command.sessionId());
        if (sessionOpt.isEmpty()) {
            log.warn("{} Game session {} could not be found.", SESSION_NOT_FOUND.tag(), command.sessionId());
            return new AddPlayerResult.SessionNotFound(command.sessionId());
        }

        GameSession session = sessionOpt.get();

        if (session.getState() != GameSessionState.CREATED
            && session.getState() != GameSessionState.PENDING
            && session.getState() != GameSessionState.SEARCHING_PLAYER
            && session.getState() != GameSessionState.JOINED
            && session.getState() != GameSessionState.STARTED
            && session.getState() != GameSessionState.RESUMED) {
            log.warn("{} Session {} is in non-joinable state {}.",
                SESSION_NOT_JOINABLE.tag(), session.getSessionId(), session.getState());
            return new AddPlayerResult.SessionNotJoinable(session.getSessionId());
        }

        if (command.slotNumber() < 1 || command.slotNumber() > 4) {
            log.warn("{} Slot {} in session {} is invalid.",
                INVALID_SLOT.tag(), command.slotNumber(), session.getSessionId());
            return new AddPlayerResult.InvalidSlot(command.slotNumber());
        }

        if (session.getPlayers().stream().anyMatch(p -> p.getSlotNumber() == command.slotNumber())) {
            log.warn("{} Slot {} in session {} is already occupied.",
                SLOT_ALREADY_OCCUPIED.tag(), command.slotNumber(), session.getSessionId());
            return new AddPlayerResult.SlotAlreadyOccupied(command.slotNumber());
        }

        if (command.requiresRegisteredUser() && command.userId() == null) {
            log.warn("{} Registered user is required in session {}.",
                REGISTERED_USER_REQUIRED.tag(), session.getSessionId());
            return new AddPlayerResult.RegisteredUserRequired(command.sessionId());
        }

        if (command.userId() != null && session.getPlayers().stream().anyMatch(p -> Objects.equals(p.getUserId(), command.userId()))) {
            log.warn("{} User {} already registered in session {}.",
                USER_ALREADY_REGISTERED.tag(), command.userId(), session.getSessionId());
            return new AddPlayerResult.UserAlreadyRegistered(command.userId());
        }

        session.addPlayer(
            command.slotNumber(),
            command.userId(),
            command.guestInitials(),
            command.requiresRegisteredUser()
        );

        gameSessionRepository.save(session);
        lobbyNotifier.notifyLobbyUpdated(session.getSessionId());

        return new AddPlayerResult.Success(session.getSessionId(), command.slotNumber());
    }
}

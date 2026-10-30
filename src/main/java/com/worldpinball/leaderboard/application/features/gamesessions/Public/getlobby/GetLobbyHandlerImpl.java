package com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby;

import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.dtos.LobbySlotDto;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.dtos.SessionLobbyDto;
import com.worldpinball.leaderboard.application.features.gamesessions.Public.getlobby.queries.GetLobbyQuery;
import com.worldpinball.leaderboard.common.EventId;
import com.worldpinball.leaderboard.domain.entities.GameSession;
import com.worldpinball.leaderboard.domain.enums.GameSlotStatus;
import com.worldpinball.leaderboard.infrastructure.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetLobbyHandlerImpl implements GetLobbyHandler {

    private static final EventId SESSION_NOT_FOUND = new EventId(2700, "SessionNotFound");

    private final GameSessionRepository gameSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public GetLobbyResult handle(GetLobbyQuery query) {
        Optional<GameSession> sessionOpt = gameSessionRepository.findByIdWithPlayers(query.sessionId());

        if (sessionOpt.isEmpty()) {
            log.warn("{} Game session {} could not be found.", SESSION_NOT_FOUND.tag(), query.sessionId());
            return new GetLobbyResult.SessionNotFound(query.sessionId());
        }

        GameSession session = sessionOpt.get();
        List<LobbySlotDto> slots = session.getSlots().stream()
                .map(slot -> new LobbySlotDto(
                        slot.getSlotNumber(),
                        slot.getStatus() == GameSlotStatus.Occupied,
                        slot.getStatus(),
                        slot.getUserId(),
                        slot.getGuestInitials()
                ))
                .toList();

        SessionLobbyDto lobbyDto = new SessionLobbyDto(
                session.getSessionId(),
                session.getState().name(),
                slots
        );

        return new GetLobbyResult.Success(lobbyDto);
    }
}

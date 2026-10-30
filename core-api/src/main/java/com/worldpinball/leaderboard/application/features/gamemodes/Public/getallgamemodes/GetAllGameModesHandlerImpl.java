package com.worldpinball.leaderboard.application.features.gamemodes.Public.getallgamemodes;

import com.worldpinball.leaderboard.domain.entities.GameMode;
import com.worldpinball.leaderboard.infrastructure.GameModeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetAllGameModesHandlerImpl implements GetAllGameModesHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetAllGameModesHandlerImpl.class);

    private final GameModeRepository gameModeRepository;

    public GetAllGameModesHandlerImpl(GameModeRepository gameModeRepository) {
        this.gameModeRepository = gameModeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GetAllGameModesResult handle(GetAllGameModesQuery query) {
        logger.info("[3200:FetchingGameModes] Fetching all game modes.");
        List<GameMode> modes = gameModeRepository.findAllByActiveTrueOrderByNameAsc();

        if (modes.isEmpty()) {
            logger.warn("[3201:NoGameModesFound] No game modes found in the system.");
            return new GetAllGameModesResult.NotFound();
        }

        List<GameModePublicDto> dtos = modes.stream()
                .map(m -> new GameModePublicDto(
                        m.getModeId(),
                        m.getCode(),
                        m.getName(),
                        m.getDescription()
                ))
                .toList();

        return new GetAllGameModesResult.Success(dtos);
    }
}

package com.worldpinball.leaderboard.application.features.matchmaking.Public.joinqueue;

import com.worldpinball.leaderboard.application.services.MatchmakingQueueService;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JoinMatchmakingQueueHandlerImpl implements JoinMatchmakingQueueHandler {

    private static final Logger logger = LoggerFactory.getLogger(JoinMatchmakingQueueHandlerImpl.class);

    private final MachineRepository machineRepository;
    private final MatchmakingQueueService matchmakingQueueService;

    public JoinMatchmakingQueueHandlerImpl(
            MachineRepository machineRepository,
            MatchmakingQueueService matchmakingQueueService
    ) {
        this.machineRepository = machineRepository;
        this.matchmakingQueueService = matchmakingQueueService;
    }

    @Override
    public JoinMatchmakingQueueResult handle(JoinMatchmakingQueueCommand cmd) {
        if (!machineRepository.existsById(cmd.machineId())) {
            logger.warn("[2700:MachineNotFound] Matchmaking > Machine {}: MachineId was not found.", cmd.machineId());
            return new JoinMatchmakingQueueResult.MachineNotFound(cmd.machineId());
        }

        matchmakingQueueService.enqueue(cmd.machineId().toString(), cmd.playerId());
        return new JoinMatchmakingQueueResult.Success();
    }
}

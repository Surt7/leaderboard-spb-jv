package com.worldpinball.leaderboard.application.features.matchmaking.Public.leavequeue;

import com.worldpinball.leaderboard.application.services.MatchmakingQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LeaveMatchmakingQueueHandlerImpl implements LeaveMatchmakingQueueHandler {

    private static final Logger logger = LoggerFactory.getLogger(LeaveMatchmakingQueueHandlerImpl.class);

    private final MatchmakingQueueService matchmakingQueueService;

    public LeaveMatchmakingQueueHandlerImpl(MatchmakingQueueService matchmakingQueueService) {
        this.matchmakingQueueService = matchmakingQueueService;
    }

    @Override
    public LeaveMatchmakingQueueResult handle(LeaveMatchmakingQueueCommand cmd) {
        boolean removed = matchmakingQueueService.remove(cmd.machineId().toString(), cmd.playerId());

        if (!removed) {
            logger.warn("[2701:PlayerNotInQueueEvent] Matchmaking > Machine {}: Player {} was not in queue.",
                    cmd.machineId(), cmd.playerId());
            return new LeaveMatchmakingQueueResult.PlayerNotInQueue();
        }

        return new LeaveMatchmakingQueueResult.Success();
    }
}

package com.worldpinball.leaderboard.application.services;

import com.worldpinball.leaderboard.common.EventId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@Slf4j
public class LobbyNotificationServiceImpl implements LobbyNotificationService {

    public static final String REDIS_LOBBY_CHANNEL = "worldpinball:lobby:updates";

    private static final EventId LOBBY_UPDATED = new EventId(2901, "LobbyUpdated");
    private static final EventId SESSION_LAUNCHED = new EventId(2902, "SessionLaunched");
    private static final EventId SESSION_ABORTED = new EventId(2903, "SessionAborted");
    private static final EventId SCORE_SUBMITTED = new EventId(2904, "ScoreSubmitted");
    private static final EventId MATCH_FOUND = new EventId(2905, "MatchFound");

    private final ConcurrentHashMap<UUID, Set<Consumer<UUID>>> subscribers = new ConcurrentHashMap<>();
    private final Set<Consumer<UUID>> globalSubscribers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Long> recentLocalBroadcasts = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;

    public LobbyNotificationServiceImpl(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void notifyLocalSubscribers(UUID sessionId) {
        Set<Consumer<UUID>> sessionSubs = subscribers.get(sessionId);
        if (sessionSubs != null) {
            for (Consumer<UUID> sub : sessionSubs) {
                try {
                    sub.accept(sessionId);
                } catch (Exception e) {
                    log.warn("Error notifying subscriber for session {}: {}", sessionId, e.getMessage());
                }
            }
        }
        for (Consumer<UUID> sub : globalSubscribers) {
            try {
                sub.accept(sessionId);
            } catch (Exception e) {
                log.warn("Error notifying global subscriber for session {}: {}", sessionId, e.getMessage());
            }
        }
    }

    public void onRedisLobbyUpdated(UUID sessionId) {
        Long lastLocal = recentLocalBroadcasts.get(sessionId);
        if (lastLocal != null && (System.currentTimeMillis() - lastLocal) < 1000) {
            // Local subscribers were already notified less than 1s ago by this instance
            return;
        }
        log.info("{} Multi-replica sync: Received Redis update for session {}", LOBBY_UPDATED.tag(), sessionId);
        notifyLocalSubscribers(sessionId);
    }

    @Override
    public void notifyLobbyUpdated(UUID sessionId) {
        log.info("{} Notification broadcast: Lobby updated for session {}.", LOBBY_UPDATED.tag(), sessionId);
        recentLocalBroadcasts.put(sessionId, System.currentTimeMillis());
        if (recentLocalBroadcasts.size() > 500) {
            long now = System.currentTimeMillis();
            recentLocalBroadcasts.entrySet().removeIf(e -> (now - e.getValue()) > 10000);
        }

        // 1. Notify local subscribers on this instance
        notifyLocalSubscribers(sessionId);

        // 2. Broadcast across all replicas via Redis Pub/Sub
        if (redisTemplate != null) {
            try {
                redisTemplate.convertAndSend(REDIS_LOBBY_CHANNEL, sessionId.toString());
            } catch (Exception e) {
                log.warn("Failed to publish lobby update to Redis: {}", e.getMessage());
            }
        }
    }

    @Override
    public void notifySessionLaunched(UUID sessionId) {
        log.info("{} Notification broadcast: Session {} launched.", SESSION_LAUNCHED.tag(), sessionId);
        notifyLobbyUpdated(sessionId);
    }

    @Override
    public void notifySessionAborted(UUID sessionId) {
        log.info("{} Notification broadcast: Session {} aborted.", SESSION_ABORTED.tag(), sessionId);
        notifyLobbyUpdated(sessionId);
    }

    @Override
    public void notifyScoreSubmitted(UUID sessionId, int slotNumber, long score) {
        log.info("{} Notification broadcast: Score {} submitted for session {} slot {}.",
            SCORE_SUBMITTED.tag(), score, sessionId, slotNumber);
        notifyLobbyUpdated(sessionId);
    }

    @Override
    public void notifyMatchFound(UUID sessionId, UUID machineId) {
        log.info("{} Notification broadcast: Match found for session {} on machine {}.",
            MATCH_FOUND.tag(), sessionId, machineId);
        notifyLobbyUpdated(sessionId);
    }

    @Override
    public Runnable subscribe(UUID sessionId, Consumer<UUID> consumer) {
        subscribers.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(consumer);
        return () -> {
            Set<Consumer<UUID>> set = subscribers.get(sessionId);
            if (set != null) {
                set.remove(consumer);
                if (set.isEmpty()) {
                    subscribers.remove(sessionId);
                }
            }
        };
    }

    @Override
    public Runnable subscribeAll(Consumer<UUID> consumer) {
        globalSubscribers.add(consumer);
        return () -> globalSubscribers.remove(consumer);
    }
}

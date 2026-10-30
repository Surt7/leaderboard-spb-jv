package com.worldpinball.leaderboard.application.services;

import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryMatchmakingQueueService implements MatchmakingQueueService {

    private final Map<String, Queue<UUID>> queues = new ConcurrentHashMap<>();

    @Override
    public synchronized void enqueue(String machineId, UUID playerId) {
        Queue<UUID> queue = queues.computeIfAbsent(machineId, k -> new LinkedList<>());
        if (!queue.contains(playerId)) {
            queue.add(playerId);
        }
    }

    @Override
    public synchronized boolean remove(String machineId, UUID playerId) {
        Queue<UUID> queue = queues.get(machineId);
        if (queue == null) {
            return false;
        }
        return queue.remove(playerId);
    }

    @Override
    public synchronized Optional<UUID> dequeueNext(String machineId) {
        Queue<UUID> queue = queues.get(machineId);
        if (queue == null || queue.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(queue.poll());
    }
}

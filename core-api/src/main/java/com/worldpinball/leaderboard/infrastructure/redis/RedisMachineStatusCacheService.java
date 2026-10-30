package com.worldpinball.leaderboard.infrastructure.redis;

import com.worldpinball.leaderboard.application.services.MachineStatusCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Mirror of C#'s RedisMachineStatusCacheService.
 */
@Service
@RequiredArgsConstructor
public class RedisMachineStatusCacheService implements MachineStatusCacheService {

    private final StringRedisTemplate redisTemplate;

    private static String getKey(UUID machineId) {
        return "machine:status:" + machineId;
    }

    @Override
    public Optional<String> getStatus(UUID machineId) {
        String val = redisTemplate.opsForValue().get(getKey(machineId));
        if (val == null || val.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(val);
    }

    @Override
    public void setStatus(UUID machineId, String status, Duration ttl) {
        redisTemplate.opsForValue().set(getKey(machineId), status, ttl);
    }

    @Override
    public void invalidateStatus(UUID machineId) {
        redisTemplate.delete(getKey(machineId));
    }
}

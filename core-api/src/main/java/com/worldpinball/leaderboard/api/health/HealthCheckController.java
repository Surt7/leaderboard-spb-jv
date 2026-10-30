package com.worldpinball.leaderboard.api.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mirrors C# HealthCheckExtensions + HealthCheckResponseWriter:
 * Exposes /api/health, /health, and /alive endpoints returning structured health status
 * for PostgreSQL, Redis, and self liveness.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Health", description = "Infrastructure readiness and liveness probes")
public class HealthCheckController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    public record CheckEntry(
            String name,
            String status,
            String description,
            String duration
    ) {}

    @GetMapping({"/api/health", "/health"})
    @Operation(summary = "Readiness health check (PostgreSQL, Redis, self)")
    public ResponseEntity<Map<String, Object>> health() {
        Instant totalStart = Instant.now();
        List<CheckEntry> checks = new ArrayList<>();
        boolean allHealthy = true;

        // 1. Self check
        checks.add(new CheckEntry("self", "Healthy", null, "00:00:00.0000100"));

        // 2. PostgreSQL check
        Instant dbStart = Instant.now();
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(2);
            Duration d = Duration.between(dbStart, Instant.now());
            if (valid) {
                checks.add(new CheckEntry("PostgreSQL", "Healthy", null, formatDuration(d)));
            } else {
                allHealthy = false;
                checks.add(new CheckEntry("PostgreSQL", "Unhealthy", "Connection validation failed", formatDuration(d)));
            }
        } catch (Exception ex) {
            allHealthy = false;
            Duration d = Duration.between(dbStart, Instant.now());
            checks.add(new CheckEntry("PostgreSQL", "Unhealthy", ex.getMessage(), formatDuration(d)));
        }

        // 3. Redis check
        Instant redisStart = Instant.now();
        try {
            String pong = redisTemplate.execute((RedisConnection connection) -> connection.ping());
            Duration d = Duration.between(redisStart, Instant.now());
            if ("PONG".equalsIgnoreCase(pong)) {
                checks.add(new CheckEntry("Redis", "Healthy", null, formatDuration(d)));
            } else {
                allHealthy = false;
                checks.add(new CheckEntry("Redis", "Unhealthy", "Unexpected PING response: " + pong, formatDuration(d)));
            }
        } catch (Exception ex) {
            allHealthy = false;
            Duration d = Duration.between(redisStart, Instant.now());
            checks.add(new CheckEntry("Redis", "Unhealthy", ex.getMessage(), formatDuration(d)));
        }

        Duration totalDuration = Duration.between(totalStart, Instant.now());
        String overallStatus = allHealthy ? "Healthy" : "Unhealthy";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", overallStatus);
        body.put("checks", checks);
        body.put("totalDuration", formatDuration(totalDuration));

        return ResponseEntity
                .status(allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }

    @GetMapping("/alive")
    @Operation(summary = "Liveness probe (self)")
    public ResponseEntity<Map<String, Object>> alive() {
        return ResponseEntity.ok(Map.of(
                "status", "Healthy",
                "checks", List.of(new CheckEntry("self", "Healthy", null, "00:00:00.0000100"))
        ));
    }

    private static String formatDuration(Duration d) {
        long millis = d.toMillis();
        return String.format("00:00:%02d.%03d0000", (millis / 1000) % 60, millis % 1000);
    }
}

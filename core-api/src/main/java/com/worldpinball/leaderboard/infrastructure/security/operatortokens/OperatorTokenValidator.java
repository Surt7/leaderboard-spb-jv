package com.worldpinball.leaderboard.infrastructure.security.operatortokens;

import com.worldpinball.leaderboard.domain.entities.OperatorToken;
import com.worldpinball.leaderboard.infrastructure.OperatorTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates operator tokens against an L1 in-memory cache (5-minute TTL) and PostgreSQL fallback.
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.OperatorTokens.OperatorTokenValidator}.
 */
@Service
public class OperatorTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(OperatorTokenValidator.class);
    private static final Duration CACHE_DURATION = Duration.ofMinutes(5);

    private record CachedToken(OperatorToken token, Instant expiresAtUtc) {}

    private final OperatorTokenRepository operatorTokenRepository;
    private final Clock clock;
    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();

    @Autowired
    public OperatorTokenValidator(OperatorTokenRepository operatorTokenRepository) {
        this(operatorTokenRepository, Clock.systemUTC());
    }

    public OperatorTokenValidator(OperatorTokenRepository operatorTokenRepository, Clock clock) {
        this.operatorTokenRepository = operatorTokenRepository;
        this.clock = clock;
    }

    public Optional<OperatorToken> validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() < 16) {
            return Optional.empty();
        }

        String tokenHash = OperatorToken.hashToken(rawToken);
        String cacheKey = "optok_" + tokenHash;
        Instant nowUtc = clock.instant();

        // 1. Lookup in in-memory cache
        CachedToken cached = cache.get(cacheKey);
        if (cached != null) {
            if (nowUtc.isBefore(cached.expiresAtUtc()) && cached.token().isValid(nowUtc)) {
                return Optional.of(cached.token());
            }
            cache.remove(cacheKey);
            return Optional.empty();
        }

        // 2. Query database for matching token
        Optional<OperatorToken> tokenOpt = operatorTokenRepository.findByTokenHash(tokenHash);
        if (tokenOpt.isEmpty()) {
            String prefix = tokenHash.length() >= 8 ? tokenHash.substring(0, 8) : tokenHash;
            log.warn("Operator token not found for hash prefix {}", prefix);
            return Optional.empty();
        }

        OperatorToken token = tokenOpt.get();
        if (!token.isValid(nowUtc)) {
            log.warn("Operator token {} ({}) is inactive, expired, or revoked", token.getName(), token.getPrefix());
            return Optional.empty();
        }

        // 3. Store valid token in memory cache
        cache.put(cacheKey, new CachedToken(token, nowUtc.plus(CACHE_DURATION)));
        return Optional.of(token);
    }
}

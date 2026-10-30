package com.worldpinball.leaderboard.infrastructure.security;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class SensitiveDataMasker {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "authorization", "apikey", "secret", "token"
    );

    public String mask(String key, String value) {
        if (key != null && SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
            return "[REDACTED]";
        }
        return value;
    }
}

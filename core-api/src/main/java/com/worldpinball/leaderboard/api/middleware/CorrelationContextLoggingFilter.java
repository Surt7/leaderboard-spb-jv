package com.worldpinball.leaderboard.api.middleware;

import com.worldpinball.leaderboard.infrastructure.security.SensitiveDataMasker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Mirrors C# CorrelationContextLoggingMiddleware:
 * Establishes an enriched SLF4J MDC logging scope with CorrelationId and masked GameSessionId
 * so all log entries across handlers can be correlated in Grafana Loki / OpenTelemetry.
 */
@Component
@RequiredArgsConstructor
public class CorrelationContextLoggingFilter extends OncePerRequestFilter {

    private final SensitiveDataMasker masker;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("CorrelationId", correlationId);
        response.setHeader("X-Correlation-Id", correlationId);

        String rawSessionId = extractSessionId(request);
        if (rawSessionId != null && !rawSessionId.isBlank()) {
            MDC.put("GameSessionId", masker.mask("GameSessionId", rawSessionId));
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("CorrelationId");
            MDC.remove("GameSessionId");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractSessionId(HttpServletRequest request) {
        Object uriVars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVars instanceof Map<?, ?> map) {
            Object sessionId = map.get("sessionId");
            if (sessionId != null) {
                return sessionId.toString();
            }
        }
        String headerVal = request.getHeader("X-Session-Id");
        if (headerVal != null && !headerVal.isBlank()) {
            return headerVal;
        }
        return null;
    }
}

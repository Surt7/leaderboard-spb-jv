package com.worldpinball.leaderboard.api.filters.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyStore idempotencyStore;
    private final RequestMappingHandlerMapping handlerMapping;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(
            IdempotencyStore idempotencyStore,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            ObjectMapper objectMapper
    ) {
        this.idempotencyStore = idempotencyStore;
        this.handlerMapping = handlerMapping;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Idempotent idempotent = resolveIdempotentAnnotation(request);
        if (idempotent == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(idempotent.headerName());
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            if (idempotent.requireHeader()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                String errorJson = objectMapper.writeValueAsString(
                        Map.of("error", "Header '" + idempotent.headerName() + "' is required for this endpoint.")
                );
                response.getWriter().write(errorJson);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String cacheKey = idempotencyKey.trim();

        Optional<IdempotentResponse> cachedResponse = idempotencyStore.get(cacheKey);
        if (cachedResponse.isPresent()) {
            IdempotentResponse cached = cachedResponse.get();
            log.info("Idempotency HIT for key {}", cacheKey);
            response.setStatus(cached.statusCode());
            response.setContentType(cached.contentType() != null ? cached.contentType() : "application/json");
            if (cached.body() != null && !cached.body().isEmpty()) {
                response.getWriter().write(cached.body());
            }
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);

        int statusCode = responseWrapper.getStatus();
        if (statusCode >= 200 && statusCode < 300) {
            String body = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            IdempotentResponse responseToCache = new IdempotentResponse(
                    statusCode,
                    responseWrapper.getContentType() != null ? responseWrapper.getContentType() : "application/json",
                    body
            );
            idempotencyStore.set(
                    cacheKey,
                    responseToCache,
                    Duration.ofMinutes(idempotent.expirationMinutes())
            );
            log.info("Idempotency MISS -> Stored response for key {}", cacheKey);
        }

        responseWrapper.copyBodyToResponse();
    }

    private Idempotent resolveIdempotentAnnotation(HttpServletRequest request) {
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            if (chain != null && chain.getHandler() instanceof HandlerMethod handlerMethod) {
                Idempotent methodAttr = AnnotatedElementUtils.findMergedAnnotation(
                        handlerMethod.getMethod(), Idempotent.class);
                if (methodAttr != null) {
                    return methodAttr;
                }
                return AnnotatedElementUtils.findMergedAnnotation(
                        handlerMethod.getBeanType(), Idempotent.class);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

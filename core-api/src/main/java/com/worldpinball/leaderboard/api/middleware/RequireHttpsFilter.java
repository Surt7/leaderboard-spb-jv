package com.worldpinball.leaderboard.api.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldpinball.leaderboard.api.errors.HttpsRequiredProblemDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RequireHttpsFilter extends OncePerRequestFilter {

    private final Environment environment;
    private final ObjectMapper objectMapper;

    @Value("${app.security.require-https:false}")
    private boolean requireHttps;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        boolean isHttps = request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));

        if (requireHttps && !isHttps) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType("application/problem+json");
            ProblemDetail problem = HttpsRequiredProblemDetails.create(
                    request,
                    MDC.get("CorrelationId"),
                    environment
            );
            response.getWriter().write(objectMapper.writeValueAsString(problem));
            return;
        }

        filterChain.doFilter(request, response);
    }
}

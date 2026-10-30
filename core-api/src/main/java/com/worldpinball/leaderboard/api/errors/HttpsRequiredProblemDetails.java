package com.worldpinball.leaderboard.api.errors;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HttpsRequiredProblemDetails {

    private HttpsRequiredProblemDetails() {}

    public static ProblemDetail create(HttpServletRequest request, String correlationId, Environment env) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "HTTP requests are not allowed. Please use HTTPS."
        );
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getRequestURI() + (correlationId != null ? "?trace=" + correlationId : "")));

        if (env != null && env.acceptsProfiles(Profiles.of("dev", "default"))) {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Host", request.getHeader("Host"));
            headers.put("User-Agent", request.getHeader("User-Agent"));
            headers.put("X-Forwarded-Proto", request.getHeader("X-Forwarded-Proto"));
            headers.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
            problem.setProperty("headers", headers);
        }

        return problem;
    }
}

package com.worldpinball.leaderboard.api.errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HttpsRequiredProblemDetails Tests")
class HttpsRequiredProblemDetailsTest {

    @Mock
    private Environment environment;

    @Test
    @DisplayName("When in production, does not include headers in properties")
    void create_whenInProduction_shouldNotIncludeHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/sessions");

        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        ProblemDetail problem = HttpsRequiredProblemDetails.create(request, "trace-123", environment);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).isNull();
    }

    @Test
    @DisplayName("When in development, includes headers in properties")
    void create_whenInDevelopment_shouldIncludeHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/sessions");
        request.addHeader("Host", "localhost:8080");
        request.addHeader("User-Agent", "PinballClient/1.0");

        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        ProblemDetail problem = HttpsRequiredProblemDetails.create(request, "trace-123", environment);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties()).containsKey("headers");
    }
}

package com.worldpinball.leaderboard.api.middleware;

import com.worldpinball.leaderboard.infrastructure.security.SensitiveDataMasker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationContextLoggingFilter Tests")
class CorrelationContextLoggingFilterTest {

    @Mock
    private SensitiveDataMasker masker;

    @Mock
    private FilterChain filterChain;

    private CorrelationContextLoggingFilter sut;

    @BeforeEach
    void setUp() {
        sut = new CorrelationContextLoggingFilter(masker);
    }

    @Test
    @DisplayName("When request arrives, populates CorrelationId in MDC and response header and calls filterChain")
    void doFilter_whenRequestArrives_shouldSetCorrelationIdAndCallNext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcCorrelationId = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcCorrelationId.set(MDC.get("CorrelationId"));
        };

        sut.doFilter(request, response, chain);

        assertThat(mdcCorrelationId.get()).isNotNull();
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(mdcCorrelationId.get());
        assertThat(MDC.get("CorrelationId")).isNull(); // cleaned up in finally block
    }

    @Test
    @DisplayName("When X-Session-Id header present, includes masked session in MDC")
    void doFilter_whenSessionHeaderPresent_shouldIncludeMaskedSessionInMDC() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Session-Id", "session-guid-123");

        when(masker.mask(eq("GameSessionId"), eq("session-guid-123"))).thenReturn("masked-session-123");

        AtomicReference<String> mdcSessionId = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcSessionId.set(MDC.get("GameSessionId"));
        };

        sut.doFilter(request, response, chain);

        assertThat(mdcSessionId.get()).isEqualTo("masked-session-123");
        verify(masker).mask("GameSessionId", "session-guid-123");
    }

    @Test
    @DisplayName("When sessionId in route attributes, includes masked session in MDC")
    void doFilter_whenSessionIdInRoute_shouldIncludeMaskedSessionInMDC() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        UUID sessionId = UUID.randomUUID();
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("sessionId", sessionId.toString()));

        when(masker.mask(eq("GameSessionId"), eq(sessionId.toString()))).thenReturn("masked-route-session");

        AtomicReference<String> mdcSessionId = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcSessionId.set(MDC.get("GameSessionId"));
        };

        sut.doFilter(request, response, chain);

        assertThat(mdcSessionId.get()).isEqualTo("masked-route-session");
    }
}

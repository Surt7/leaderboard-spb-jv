package com.worldpinball.leaderboard.integration;

import com.worldpinball.leaderboard.infrastructure.security.SensitiveDataMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.outbox.worker.enabled=false",
        "app.outbox.cleanup.enabled=false"
})
@AutoConfigureMockMvc
class MiddlewareAndHealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SensitiveDataMasker sensitiveDataMasker;

    @Test
    @DisplayName("GET /api/health returns Healthy status for self, PostgreSQL, and Redis")
    void healthEndpoint_returnsHealthyReport() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.status").value("Healthy"))
                .andExpect(jsonPath("$.checks[*].name", hasItems("self", "PostgreSQL", "Redis")))
                .andExpect(jsonPath("$.totalDuration").isNotEmpty());
    }

    @Test
    @DisplayName("GET /alive returns Healthy liveness status")
    void aliveEndpoint_returnsHealthy() throws Exception {
        mockMvc.perform(get("/alive")
                        .header("X-Correlation-Id", "custom-corr-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "custom-corr-123"))
                .andExpect(jsonPath("$.status").value("Healthy"));
    }

    @Test
    @DisplayName("SensitiveDataMasker masks sensitive keys and preserves safe keys")
    void sensitiveDataMasker_redactsSensitiveKeysOnly() {
        assertThat(sensitiveDataMasker.mask("password", "Surt7*!")).isEqualTo("[REDACTED]");
        assertThat(sensitiveDataMasker.mask("Authorization", "Bearer eyJ...")).isEqualTo("[REDACTED]");
        assertThat(sensitiveDataMasker.mask("Token", "tok-123")).isEqualTo("[REDACTED]");
        assertThat(sensitiveDataMasker.mask("GameSessionId", "session-abc")).isEqualTo("session-abc");
    }
}

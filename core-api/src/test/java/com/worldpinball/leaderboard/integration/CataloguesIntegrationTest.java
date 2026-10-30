package com.worldpinball.leaderboard.integration;

import com.worldpinball.leaderboard.domain.entities.GameMode;
import com.worldpinball.leaderboard.domain.entities.IpLicense;
import com.worldpinball.leaderboard.domain.entities.Publisher;
import com.worldpinball.leaderboard.infrastructure.GameModeRepository;
import com.worldpinball.leaderboard.infrastructure.IpLicenseRepository;
import com.worldpinball.leaderboard.infrastructure.PublisherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CataloguesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameModeRepository gameModeRepository;

    @Autowired
    private IpLicenseRepository ipLicenseRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Test
    @DisplayName("GET /api/v1/game-modes - should return active game modes sorted by name")
    void shouldReturnActiveGameModes() throws Exception {
        // Arrange
        GameMode modeB = new GameMode("classic", "B Classic Mode", "Standard 3-ball play", true, Instant.now(), UUID.randomUUID());
        GameMode modeA = new GameMode("timed", "A Timed Rush", "Fast play", true, Instant.now(), UUID.randomUUID());
        GameMode modeInactive = new GameMode("legacy", "Legacy Mode", "Old mode", false, Instant.now(), UUID.randomUUID());

        gameModeRepository.save(modeB);
        gameModeRepository.save(modeA);
        gameModeRepository.save(modeInactive);

        // Act & Assert
        mockMvc.perform(get("/api/v1/game-modes")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].name", hasItems("A Timed Rush", "B Classic Mode")))
                .andExpect(jsonPath("$[*].name", not(hasItem("Legacy Mode"))));
    }

    @Test
    @DisplayName("GET /api/v1/ip-licenses - should return active IP licenses with publisher")
    void shouldReturnActiveIpLicenses() throws Exception {
        // Arrange
        String uniqueSlug = "medieval-madness-" + UUID.randomUUID().toString().substring(0, 8);
        Publisher publisher = new Publisher("Williams Pinball", "williams-" + UUID.randomUUID().toString().substring(0, 8), Instant.now());
        publisherRepository.save(publisher);

        IpLicense license = new IpLicense(
                "Medieval Madness",
                uniqueSlug,
                publisher.getPublisherId(),
                true,
                Instant.now(),
                UUID.randomUUID()
        );
        ipLicenseRepository.save(license);

        // Act & Assert
        mockMvc.perform(get("/api/v1/ip-licenses")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[?(@.slug == '" + uniqueSlug + "')].name").value("Medieval Madness"))
                .andExpect(jsonPath("$[?(@.slug == '" + uniqueSlug + "')].publisher.name").value("Williams Pinball"));
    }
}

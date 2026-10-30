package com.worldpinball.leaderboard.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Reference Entities Unit Tests (GameMode, Publisher, Region)")
class ReferenceEntitiesTest {

    @Nested
    @DisplayName("GameMode Tests")
    class GameModeTests {

        @Test
        void constructor_whenValid_shouldInitializeProperly() {
            GameMode mode = new GameMode("classic", "Classic Mode", "Standard pinball mode", true, Instant.now(), null);

            assertThat(mode.getModeId()).isNotNull();
            assertThat(mode.getCode()).isEqualTo("classic");
            assertThat(mode.getName()).isEqualTo("Classic Mode");
            assertThat(mode.getDescription()).isEqualTo("Standard pinball mode");
            assertThat(mode.isActive()).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void constructor_whenCodeInvalid_throwsIllegalArgumentException(String invalidCode) {
            assertThatThrownBy(() -> new GameMode(invalidCode, "Classic", null, true, Instant.now(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void constructor_whenNameInvalid_throwsIllegalArgumentException(String invalidName) {
            assertThatThrownBy(() -> new GameMode("code", invalidName, null, true, Instant.now(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void activateAndDeactivate_shouldUpdateStatusAndTimestamp() {
            GameMode mode = new GameMode("tourney", "Tournament Mode", null, true, Instant.now(), null);
            Instant now = Instant.now();

            mode.deactivate(now);
            assertThat(mode.isActive()).isFalse();
            assertThat(mode.getUpdatedAtUtc()).isEqualTo(now);

            Instant resumeTime = now.plus(1, ChronoUnit.HOURS);
            mode.activate(resumeTime);
            assertThat(mode.isActive()).isTrue();
            assertThat(mode.getUpdatedAtUtc()).isEqualTo(resumeTime);
        }
    }

    @Nested
    @DisplayName("Publisher Tests")
    class PublisherTests {

        @Test
        void constructor_whenValid_shouldInitializeProperly() {
            Publisher publisher = new Publisher("Stern Pinball", "stern-pinball", Instant.now());

            assertThat(publisher.getPublisherId()).isNotNull();
            assertThat(publisher.getName()).isEqualTo("Stern Pinball");
            assertThat(publisher.getSlug()).isEqualTo("stern-pinball");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void constructor_whenNameInvalid_throwsIllegalArgumentException(String invalidName) {
            assertThatThrownBy(() -> new Publisher(invalidName, "slug", Instant.now()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void constructor_whenSlugInvalid_throwsIllegalArgumentException(String invalidSlug) {
            assertThatThrownBy(() -> new Publisher("Name", invalidSlug, Instant.now()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Region Tests")
    class RegionTests {

        @Test
        void constructor_whenValid_shouldInitializeProperly() {
            Region region = new Region("eu", "Europe", Instant.now());

            assertThat(region.getRegionId()).isNotNull();
            assertThat(region.getCode()).isEqualTo("EU");
            assertThat(region.getName()).isEqualTo("Europe");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void constructor_whenCodeInvalid_throwsIllegalArgumentException(String invalidCode) {
            assertThatThrownBy(() -> new Region(invalidCode, "Europe", Instant.now()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void constructor_whenNameInvalid_throwsIllegalArgumentException(String invalidName) {
            assertThatThrownBy(() -> new Region("EU", invalidName, Instant.now()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}

package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.PrincipalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StreamSubscription Domain Entity Unit Tests")
class StreamSubscriptionTest {

    @Test
    void constructor_whenArgumentsValid_shouldInitializeSubscription() {
        UUID subscriberId = UUID.randomUUID();
        UUID targetOrgId = UUID.randomUUID();
        Instant subscribedAtUtc = Instant.now();

        StreamSubscription subscription = new StreamSubscription(
                subscriberId,
                PrincipalType.ORGANIZATION,
                targetOrgId,
                subscribedAtUtc,
                true
        );

        assertThat(subscription.getSubscriptionId()).isNotNull();
        assertThat(subscription.getSubscriberUserId()).isEqualTo(subscriberId);
        assertThat(subscription.getTargetType()).isEqualTo(PrincipalType.ORGANIZATION);
        assertThat(subscription.getTargetPrincipalId()).isEqualTo(targetOrgId);
        assertThat(subscription.getSubscribedAtUtc()).isEqualTo(subscribedAtUtc);
        assertThat(subscription.isNotificationsEnabled()).isTrue();
    }

    @Test
    void constructor_whenSubscriberUserIdNull_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> new StreamSubscription(
                null,
                PrincipalType.ORGANIZATION,
                UUID.randomUUID(),
                Instant.now(),
                true
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage(StreamSubscription.ErrorMessages.EMPTY_SUBSCRIBER_USER_ID);
    }

    @Test
    void constructor_whenTargetPrincipalIdNull_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> new StreamSubscription(
                UUID.randomUUID(),
                PrincipalType.ORGANIZATION,
                null,
                Instant.now(),
                true
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage(StreamSubscription.ErrorMessages.EMPTY_TARGET_PRINCIPAL_ID);
    }

    @Test
    void constructor_whenUserSubscribesToThemselves_shouldThrowIllegalStateException() {
        UUID sameUserId = UUID.randomUUID();

        assertThatThrownBy(() -> new StreamSubscription(
                sameUserId,
                PrincipalType.USER,
                sameUserId,
                Instant.now(),
                true
        )).isInstanceOf(IllegalStateException.class)
          .hasMessage(StreamSubscription.ErrorMessages.CANNOT_SUBSCRIBE_TO_SELF);
    }

    @Test
    void setNotifications_whenChanged_shouldUpdateState() {
        StreamSubscription subscription = new StreamSubscription(
                UUID.randomUUID(),
                PrincipalType.ORGANIZATION,
                UUID.randomUUID(),
                Instant.now(),
                true
        );

        subscription.setNotifications(false);

        assertThat(subscription.isNotificationsEnabled()).isFalse();
    }
}

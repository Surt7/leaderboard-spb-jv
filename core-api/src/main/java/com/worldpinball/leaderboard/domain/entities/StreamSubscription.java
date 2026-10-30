package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.PrincipalType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stream_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamSubscription {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String EMPTY_SUBSCRIBER_USER_ID = "SubscriberUserId cannot be empty.";
        public static final String EMPTY_TARGET_PRINCIPAL_ID = "TargetPrincipalId cannot be empty.";
        public static final String CANNOT_SUBSCRIBE_TO_SELF = "A user cannot subscribe to themselves.";
    }

    @Id
    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "subscriber_user_id", nullable = false)
    private UUID subscriberUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30, nullable = false)
    private PrincipalType targetType;

    @Column(name = "target_principal_id", nullable = false)
    private UUID targetPrincipalId;

    @Column(name = "subscribed_at_utc", nullable = false)
    private Instant subscribedAtUtc;

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled;

    public StreamSubscription(
            UUID subscriberUserId,
            PrincipalType targetType,
            UUID targetPrincipalId,
            Instant subscribedAtUtc,
            boolean notificationsEnabled
    ) {
        if (subscriberUserId == null) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_SUBSCRIBER_USER_ID);
        }
        if (targetPrincipalId == null) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_TARGET_PRINCIPAL_ID);
        }
        if (subscriberUserId.equals(targetPrincipalId) && targetType == PrincipalType.USER) {
            throw new IllegalStateException(ErrorMessages.CANNOT_SUBSCRIBE_TO_SELF);
        }

        this.subscriptionId = UUID.randomUUID();
        this.subscriberUserId = subscriberUserId;
        this.targetType = targetType;
        this.targetPrincipalId = targetPrincipalId;
        this.subscribedAtUtc = subscribedAtUtc != null ? subscribedAtUtc : Instant.now();
        this.notificationsEnabled = notificationsEnabled;
    }

    public void setNotifications(boolean enabled) {
        this.notificationsEnabled = enabled;
    }
}

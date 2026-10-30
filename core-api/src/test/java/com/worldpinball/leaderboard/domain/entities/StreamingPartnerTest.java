package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.StreamingRequestStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StreamingPartner and StreamingAccessRequest Domain Entity Unit Tests")
class StreamingPartnerTest {

    @Test
    void constructor_whenArgumentsAreValid_shouldInitializeActivePartner() {
        UUID userId = UUID.randomUUID();
        String organization = "IFPA World Championship";
        Instant createdAtUtc = Instant.now();

        StreamingPartner partner = new StreamingPartner(userId, organization, createdAtUtc);

        assertThat(partner.getPartnerId()).isNotNull();
        assertThat(partner.getUserId()).isEqualTo(userId);
        assertThat(partner.getOrganizationName()).isEqualTo(organization);
        assertThat(partner.isActive()).isTrue();
        assertThat(partner.getCreatedAtUtc()).isEqualTo(createdAtUtc);
        assertThat(partner.getRevokedAtUtc()).isNull();
    }

    @Test
    void revoke_whenPartnerIsActive_shouldDeactivateAndSetRevokedAt() {
        Instant createdAt = Instant.now();
        StreamingPartner partner = new StreamingPartner(UUID.randomUUID(), "Stern Pro Circuit", createdAt);
        Instant revokedAt = createdAt.plus(10, ChronoUnit.DAYS);

        partner.revoke(revokedAt);

        assertThat(partner.isActive()).isFalse();
        assertThat(partner.getRevokedAtUtc()).isEqualTo(revokedAt);
    }

    @Test
    void reactivate_whenPartnerWasRevoked_shouldReactivateAndClearRevokedAt() {
        Instant createdAt = Instant.now();
        StreamingPartner partner = new StreamingPartner(UUID.randomUUID(), "Pinball Expo", createdAt);
        partner.revoke(createdAt.plus(5, ChronoUnit.DAYS));

        partner.reactivate();

        assertThat(partner.isActive()).isTrue();
        assertThat(partner.getRevokedAtUtc()).isNull();
    }

    @Test
    void requestConstructor_whenValid_shouldInitializeInPendingStatus() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        StreamingAccessRequest request = new StreamingAccessRequest(sessionId, userId, createdAt);

        assertThat(request.getRequestId()).isNotNull();
        assertThat(request.getSessionId()).isEqualTo(sessionId);
        assertThat(request.getRequestedByUserId()).isEqualTo(userId);
        assertThat(request.getStatus()).isEqualTo(StreamingRequestStatus.PENDING);
        assertThat(request.getCreatedAtUtc()).isEqualTo(createdAt);
        assertThat(request.getExpiresAtUtc()).isNull();
    }

    @Test
    void approve_whenPending_shouldSetApprovedStatusAndCalculatedExpiry() {
        StreamingAccessRequest request = new StreamingAccessRequest(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        Instant approvedAt = Instant.now();
        Duration duration = Duration.ofHours(2);

        request.approve(approvedAt, duration);

        assertThat(request.getStatus()).isEqualTo(StreamingRequestStatus.APPROVED);
        assertThat(request.getReviewedAtUtc()).isEqualTo(approvedAt);
        assertThat(request.getExpiresAtUtc()).isEqualTo(approvedAt.plus(duration));
    }

    @Test
    void reject_whenPending_shouldSetRejectedStatusAndReason() {
        StreamingAccessRequest request = new StreamingAccessRequest(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        Instant reviewedAt = Instant.now();
        String reason = "Uncertified tournament format.";

        request.reject(reason, reviewedAt);

        assertThat(request.getStatus()).isEqualTo(StreamingRequestStatus.REJECTED);
        assertThat(request.getReviewedAtUtc()).isEqualTo(reviewedAt);
        assertThat(request.getRejectionReason()).isEqualTo(reason);
        assertThat(request.getExpiresAtUtc()).isNull();
    }
}

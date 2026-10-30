package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.GrantOrigin;
import com.worldpinball.leaderboard.domain.enums.GrantStatus;
import com.worldpinball.leaderboard.domain.enums.PrincipalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StreamAccessGrant Domain Entity Unit Tests")
class StreamAccessGrantTest {

    @Test
    void constructor_whenArgumentsValid_shouldInitializeActiveGrant() {
        UUID principalId = UUID.randomUUID();
        String displayName = "Stern Pinball Official";
        Instant createdAtUtc = Instant.now();
        Instant expiresAtUtc = createdAtUtc.plus(365, ChronoUnit.DAYS);
        UUID adminId = UUID.randomUUID();

        StreamAccessGrant grant = new StreamAccessGrant(
                PrincipalType.ORGANIZATION,
                principalId,
                displayName,
                GrantOrigin.WHITELIST,
                createdAtUtc,
                expiresAtUtc,
                adminId
        );

        assertThat(grant.getGrantId()).isNotNull();
        assertThat(grant.getPrincipalType()).isEqualTo(PrincipalType.ORGANIZATION);
        assertThat(grant.getPrincipalId()).isEqualTo(principalId);
        assertThat(grant.getDisplayName()).isEqualTo(displayName);
        assertThat(grant.getOrigin()).isEqualTo(GrantOrigin.WHITELIST);
        assertThat(grant.getStatus()).isEqualTo(GrantStatus.ACTIVE);
        assertThat(grant.getCreatedAtUtc()).isEqualTo(createdAtUtc);
        assertThat(grant.getExpiresAtUtc()).isEqualTo(expiresAtUtc);
        assertThat(grant.getGrantedByUserId()).isEqualTo(adminId);
        assertThat(grant.getRevokedAtUtc()).isNull();
        assertThat(grant.getRevocationReason()).isNull();
    }

    @Test
    void constructor_whenPrincipalIdNull_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> new StreamAccessGrant(
                PrincipalType.USER,
                null,
                "Player One",
                GrantOrigin.BY_APPROVAL,
                Instant.now(),
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("PrincipalId cannot be empty");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void constructor_whenDisplayNameEmptyOrNull_shouldThrowIllegalArgumentException(String invalidName) {
        assertThatThrownBy(() -> new StreamAccessGrant(
                PrincipalType.USER,
                UUID.randomUUID(),
                invalidName,
                GrantOrigin.BY_APPROVAL,
                Instant.now(),
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("DisplayName cannot be empty");
    }

    @Test
    void constructor_whenExpiresAtInPast_shouldThrowIllegalArgumentException() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> new StreamAccessGrant(
                PrincipalType.USER,
                UUID.randomUUID(),
                "Player One",
                GrantOrigin.BY_APPROVAL,
                now,
                now.minusSeconds(300),
                null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ExpiresAtUtc must be in the future");
    }

    @Test
    void revoke_whenActiveAndReasonProvided_shouldSetRevokedStatus() {
        StreamAccessGrant grant = new StreamAccessGrant(
                PrincipalType.USER,
                UUID.randomUUID(),
                "Player One",
                GrantOrigin.WHITELIST,
                Instant.now(),
                null,
                null
        );

        Instant revokedAt = Instant.now().plus(1, ChronoUnit.DAYS);
        String reason = "Partner agreement terminated.";

        grant.revoke(reason, revokedAt);

        assertThat(grant.getStatus()).isEqualTo(GrantStatus.REVOKED);
        assertThat(grant.getRevokedAtUtc()).isEqualTo(revokedAt);
        assertThat(grant.getRevocationReason()).isEqualTo(reason);
    }

    @Test
    void revoke_whenAlreadyRevoked_shouldThrowIllegalStateException() {
        StreamAccessGrant grant = new StreamAccessGrant(
                PrincipalType.USER,
                UUID.randomUUID(),
                "Player One",
                GrantOrigin.WHITELIST,
                Instant.now(),
                null,
                null
        );

        grant.revoke("First revocation", Instant.now());

        assertThatThrownBy(() -> grant.revoke("Second revocation", Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Grant is already revoked.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void revoke_whenReasonEmptyOrNull_shouldThrowIllegalArgumentException(String invalidReason) {
        StreamAccessGrant grant = new StreamAccessGrant(
                PrincipalType.USER,
                UUID.randomUUID(),
                "Player One",
                GrantOrigin.WHITELIST,
                Instant.now(),
                null,
                null
        );

        assertThatThrownBy(() -> grant.revoke(invalidReason, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revocation reason is required");
    }

    @Test
    void reactivate_whenRevoked_shouldResetToActive() {
        StreamAccessGrant grant = new StreamAccessGrant(
                PrincipalType.USER,
                UUID.randomUUID(),
                "Player One",
                GrantOrigin.WHITELIST,
                Instant.now(),
                null,
                null
        );

        grant.revoke("Violation of TOS", Instant.now());
        grant.reactivate();

        assertThat(grant.getStatus()).isEqualTo(GrantStatus.ACTIVE);
        assertThat(grant.getRevokedAtUtc()).isNull();
        assertThat(grant.getRevocationReason()).isNull();
    }

    @Test
    void isActive_whenActiveAndNotExpired_shouldReturnTrue() {
        Instant now = Instant.now();
        StreamAccessGrant grant = new StreamAccessGrant(
                PrincipalType.USER,
                UUID.randomUUID(),
                "Player One",
                GrantOrigin.WHITELIST,
                now,
                now.plus(7, ChronoUnit.DAYS),
                null
        );

        assertThat(grant.isActive(now)).isTrue();
    }

    @Test
    void isActive_whenExpired_shouldReturnFalse() {
        Instant createdAt = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant expiresAt = createdAt.plus(5, ChronoUnit.DAYS);

        StreamAccessGrant grant = new StreamAccessGrant(
                PrincipalType.USER,
                UUID.randomUUID(),
                "Player One",
                GrantOrigin.WHITELIST,
                createdAt,
                expiresAt,
                null
        );

        assertThat(grant.isActive(Instant.now())).isFalse();
    }

    @Test
    void isActive_whenRevoked_shouldReturnFalse() {
        StreamAccessGrant grant = new StreamAccessGrant(
                PrincipalType.USER,
                UUID.randomUUID(),
                "Player One",
                GrantOrigin.WHITELIST,
                Instant.now(),
                null,
                null
        );

        grant.revoke("Revocation", Instant.now());

        assertThat(grant.isActive(Instant.now())).isFalse();
    }
}

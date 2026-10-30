package com.worldpinball.leaderboard.domain.entities;

import com.worldpinball.leaderboard.domain.enums.StreamingRequestStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "streaming_access_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamingAccessRequest {

    public static final class ErrorMessages {
        private ErrorMessages() {}
        public static final String EMPTY_SESSION_ID = "SessionId cannot be empty.";
        public static final String EMPTY_USER_ID = "UserId cannot be empty.";
        public static final String INVALID_CREATED_AT = "CreatedAtUtc must be a valid date.";
        public static final String CANNOT_APPROVE_NON_PENDING = "Cannot approve a request that is not in Pending status.";
        public static final String CANNOT_REJECT_NON_PENDING = "Cannot reject a request that is not in Pending status.";
        public static final String EMPTY_REJECTION_REASON = "Rejection reason cannot be empty.";
        public static final String INVALID_DURATION = "Duration must be greater than zero.";
    }

    @Id
    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "requested_by_user_id", nullable = false)
    private UUID requestedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private StreamingRequestStatus status;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "reviewed_at_utc")
    private Instant reviewedAtUtc;

    @Column(name = "expires_at_utc")
    private Instant expiresAtUtc;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    public StreamingAccessRequest(UUID sessionId, UUID requestedByUserId, Instant createdAtUtc) {
        if (sessionId == null) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_SESSION_ID);
        }
        if (requestedByUserId == null) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_USER_ID);
        }
        if (createdAtUtc == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_CREATED_AT);
        }

        this.requestId = UUID.randomUUID();
        this.sessionId = sessionId;
        this.requestedByUserId = requestedByUserId;
        this.status = StreamingRequestStatus.PENDING;
        this.createdAtUtc = createdAtUtc;
    }

    public void approve(Instant approvedAtUtc, Duration duration) {
        if (this.status != StreamingRequestStatus.PENDING) {
            throw new IllegalStateException(ErrorMessages.CANNOT_APPROVE_NON_PENDING);
        }
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_DURATION);
        }

        this.status = StreamingRequestStatus.APPROVED;
        this.reviewedAtUtc = approvedAtUtc;
        this.expiresAtUtc = approvedAtUtc.plus(duration);
    }

    public void reject(String reason, Instant rejectedAtUtc) {
        if (this.status != StreamingRequestStatus.PENDING) {
            throw new IllegalStateException(ErrorMessages.CANNOT_REJECT_NON_PENDING);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.EMPTY_REJECTION_REASON);
        }

        this.status = StreamingRequestStatus.REJECTED;
        this.reviewedAtUtc = rejectedAtUtc;
        this.rejectionReason = reason.trim();
    }
}

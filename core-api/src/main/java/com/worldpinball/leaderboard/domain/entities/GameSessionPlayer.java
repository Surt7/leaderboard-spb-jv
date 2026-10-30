package com.worldpinball.leaderboard.domain.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_session_players")
@IdClass(GameSessionPlayerId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSessionPlayer {

    @Id
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Id
    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "guest_initials", length = 4)
    private String guestInitials;

    @Column(name = "joined_at_utc", nullable = false)
    private Instant joinedAtUtc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private GameSession session;

    public GameSessionPlayer(
        UUID sessionId,
        int slotNumber,
        UUID userId,
        String guestInitials,
        Instant joinedAtUtc
    ) {
        if (sessionId == null) {
            throw new IllegalArgumentException("SessionId cannot be null.");
        }
        if (slotNumber < 1 || slotNumber > 4) {
            throw new IllegalArgumentException("SlotNumber must be between 1 and 4.");
        }

        this.sessionId = sessionId;
        this.slotNumber = slotNumber;
        this.userId = userId;
        this.guestInitials = guestInitials != null ? guestInitials.trim().toUpperCase() : null;
        this.joinedAtUtc = joinedAtUtc != null ? joinedAtUtc : Instant.now();
    }
}

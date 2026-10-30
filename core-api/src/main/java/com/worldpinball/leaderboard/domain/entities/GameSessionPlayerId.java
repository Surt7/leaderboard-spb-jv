package com.worldpinball.leaderboard.domain.entities;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GameSessionPlayerId implements Serializable {
    private UUID sessionId;
    private Integer slotNumber;
}

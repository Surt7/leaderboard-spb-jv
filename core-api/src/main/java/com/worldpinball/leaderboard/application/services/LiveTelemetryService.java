package com.worldpinball.leaderboard.application.services;

import java.util.UUID;

public interface LiveTelemetryService {

    long ingestDelta(UUID sessionId, int slotNumber, long pointsDelta, String triggerEvent);
}

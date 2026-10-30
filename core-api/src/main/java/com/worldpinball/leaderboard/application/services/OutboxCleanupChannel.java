package com.worldpinball.leaderboard.application.services;

import java.time.Duration;

public interface OutboxCleanupChannel {
    void requestCleanup();
    boolean waitForSignalOrTimeout(Duration timeout) throws InterruptedException;
    void drain();
}

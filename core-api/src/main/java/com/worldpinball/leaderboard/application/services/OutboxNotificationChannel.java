package com.worldpinball.leaderboard.application.services;

import java.time.Duration;

public interface OutboxNotificationChannel {
    void tryNotify();
    boolean waitForSignalOrTimeout(Duration timeout) throws InterruptedException;
    void drain();
}

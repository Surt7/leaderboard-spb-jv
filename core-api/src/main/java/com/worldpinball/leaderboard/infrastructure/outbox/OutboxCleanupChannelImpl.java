package com.worldpinball.leaderboard.infrastructure.outbox;

import com.worldpinball.leaderboard.application.services.OutboxCleanupChannel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxCleanupChannelImpl implements OutboxCleanupChannel {

    private final LinkedBlockingQueue<Boolean> cleanupChannel = new LinkedBlockingQueue<>(1);

    @Override
    public void requestCleanup() {
        if (!cleanupChannel.offer(Boolean.TRUE)) {
            cleanupChannel.poll();
            cleanupChannel.offer(Boolean.TRUE);
        }
    }

    @Override
    public boolean waitForSignalOrTimeout(Duration timeout) throws InterruptedException {
        Boolean signal = cleanupChannel.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        return signal != null;
    }

    @Override
    public void drain() {
        cleanupChannel.clear();
    }
}

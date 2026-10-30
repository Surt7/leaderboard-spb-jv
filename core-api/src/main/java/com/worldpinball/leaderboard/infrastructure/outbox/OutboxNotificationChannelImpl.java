package com.worldpinball.leaderboard.infrastructure.outbox;

import com.worldpinball.leaderboard.application.services.OutboxNotificationChannel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxNotificationChannelImpl implements OutboxNotificationChannel {

    // Bounded capacity 1000 with DropWrite semantics (.offer) matching C# Channel.CreateBounded<bool>(1000, DropWrite)
    private final LinkedBlockingQueue<Boolean> channel = new LinkedBlockingQueue<>(1000);

    @Override
    public void tryNotify() {
        channel.offer(Boolean.TRUE);
    }

    @Override
    public boolean waitForSignalOrTimeout(Duration timeout) throws InterruptedException {
        Boolean signal = channel.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        return signal != null;
    }

    @Override
    public void drain() {
        channel.clear();
    }
}

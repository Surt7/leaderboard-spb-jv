package com.worldpinball.leaderboard.infrastructure.outbox;

import com.worldpinball.leaderboard.application.services.OutboxNotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxBackgroundService implements SmartLifecycle {

    private final OutboxProcessor outboxProcessor;
    private final OutboxNotificationChannel outboxNotificationChannel;

    @Value("${app.outbox.worker.enabled:true}")
    private boolean enabled;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    @Override
    public void start() {
        if (!enabled || !running.compareAndSet(false, true)) {
            return;
        }
        workerThread = Thread.ofVirtual()
                .name("outbox-worker")
                .start(this::executeLoop);
    }

    private void executeLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Wait up to 5 seconds or wake up immediately when notified via channel
                outboxNotificationChannel.waitForSignalOrTimeout(Duration.ofSeconds(5));
                outboxNotificationChannel.drain();

                outboxProcessor.processBatch();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                if (running.get()) {
                    log.error("Error while processing outbox messages.", ex);
                }
            }
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false) && workerThread != null) {
            workerThread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}

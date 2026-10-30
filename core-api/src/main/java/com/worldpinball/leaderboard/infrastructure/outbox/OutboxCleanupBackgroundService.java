package com.worldpinball.leaderboard.infrastructure.outbox;

import com.worldpinball.leaderboard.application.services.OutboxCleanupChannel;
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
public class OutboxCleanupBackgroundService implements SmartLifecycle {

    private final OutboxCleaner outboxCleaner;
    private final OutboxCleanupChannel outboxCleanupChannel;

    @Value("${app.outbox.cleanup.enabled:true}")
    private boolean enabled;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    @Override
    public void start() {
        if (!enabled || !running.compareAndSet(false, true)) {
            return;
        }
        workerThread = Thread.ofVirtual()
                .name("outbox-cleanup-worker")
                .start(this::executeLoop);
    }

    private void executeLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Wait up to 1 hour or wake up immediately when cleanup is requested
                outboxCleanupChannel.waitForSignalOrTimeout(Duration.ofHours(1));
                outboxCleanupChannel.drain();

                outboxCleaner.cleanupBatch(Duration.ofDays(7));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                if (running.get()) {
                    log.error("Error while executing outbox cleanup.", ex);
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

package com.worldpinball.leaderboard.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("LobbyNotificationServiceImpl Tests")
class LobbyNotificationServiceImplTest {

    @Test
    @DisplayName("Publishes to Redis and notifies local subscribers")
    void notifyLobbyUpdated_withRedis_publishesAndNotifiesLocal() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        LobbyNotificationServiceImpl sut = new LobbyNotificationServiceImpl(redisTemplate);

        UUID sessionId = UUID.randomUUID();
        AtomicInteger callCount = new AtomicInteger(0);

        sut.subscribe(sessionId, sid -> callCount.incrementAndGet());

        sut.notifyLobbyUpdated(sessionId);

        assertThat(callCount.get()).isEqualTo(1);
        verify(redisTemplate).convertAndSend(LobbyNotificationServiceImpl.REDIS_LOBBY_CHANNEL, sessionId.toString());
    }

    @Test
    @DisplayName("Works without Redis gracefully")
    void notifyLobbyUpdated_withoutRedis_notifiesLocalWithoutError() {
        LobbyNotificationServiceImpl sut = new LobbyNotificationServiceImpl(null);

        UUID sessionId = UUID.randomUUID();
        AtomicInteger callCount = new AtomicInteger(0);

        sut.subscribe(sessionId, sid -> callCount.incrementAndGet());
        sut.notifyLobbyUpdated(sessionId);

        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("onRedisLobbyUpdated skips recently notified local updates to prevent loops")
    void onRedisLobbyUpdated_whenRecentlyPublishedLocally_skipsDuplicate() {
        LobbyNotificationServiceImpl sut = new LobbyNotificationServiceImpl(null);

        UUID sessionId = UUID.randomUUID();
        AtomicInteger callCount = new AtomicInteger(0);

        sut.subscribe(sessionId, sid -> callCount.incrementAndGet());

        sut.notifyLobbyUpdated(sessionId);
        assertThat(callCount.get()).isEqualTo(1);

        // Redis echo arrives immediately
        sut.onRedisLobbyUpdated(sessionId);
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("onRedisLobbyUpdated notifies local subscribers for external updates")
    void onRedisLobbyUpdated_whenFromExternalReplica_notifiesLocal() {
        LobbyNotificationServiceImpl sut = new LobbyNotificationServiceImpl(null);

        UUID sessionId = UUID.randomUUID();
        AtomicInteger callCount = new AtomicInteger(0);

        sut.subscribe(sessionId, sid -> callCount.incrementAndGet());

        // Update originates from another replica
        sut.onRedisLobbyUpdated(sessionId);
        assertThat(callCount.get()).isEqualTo(1);
    }
}

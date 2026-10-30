package com.worldpinball.leaderboard.infrastructure.redis;

import com.worldpinball.leaderboard.application.services.LobbyNotificationServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Configures Redis Pub/Sub listener container for cross-replica lobby synchronization.
 * Listens to channel {@code worldpinball:lobby:updates} and dispatches events to {@link LobbyNotificationServiceImpl}.
 * Mirrors C#'s {@code InMemoryLobbyNotificationService} Redis subscription.
 */
@Configuration
@Slf4j
public class RedisPubSubConfig {

    public static final String LOBBY_UPDATES_CHANNEL = LobbyNotificationServiceImpl.REDIS_LOBBY_CHANNEL;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter lobbyUpdatesListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(lobbyUpdatesListenerAdapter, new ChannelTopic(LOBBY_UPDATES_CHANNEL));
        log.info("Registered Redis Pub/Sub listener container on channel '{}'", LOBBY_UPDATES_CHANNEL);
        return container;
    }

    @Bean
    public MessageListenerAdapter lobbyUpdatesListenerAdapter(LobbyNotificationServiceImpl notificationService) {
        return new MessageListenerAdapter((MessageListener) (message, pattern) -> {
            try {
                String body = new String(message.getBody(), StandardCharsets.UTF_8).replace("\"", "").trim();
                UUID sessionId = UUID.fromString(body);
                notificationService.onRedisLobbyUpdated(sessionId);
            } catch (Exception e) {
                log.warn("Failed to process Redis lobby update message: {}", e.getMessage());
            }
        });
    }
}

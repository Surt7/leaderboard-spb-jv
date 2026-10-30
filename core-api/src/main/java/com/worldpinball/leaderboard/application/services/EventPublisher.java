package com.worldpinball.leaderboard.application.services;

public interface EventPublisher {
    void publish(String topic, String key, String payload);
}

package com.worldpinball.leaderboard.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardKafkaConsumer {

    private final LeaderboardScoreEventProcessor processor;

    @KafkaListener(topics = "game-sessions", groupId = "worldpinball-leaderboard-updater")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        if (record == null || record.value() == null) {
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            return;
        }

        processor.processMessage(record.value());

        if (acknowledgment != null) {
            acknowledgment.acknowledge();
        }

        log.debug("Offset {} committed for partition {}.", record.offset(), record.partition());
    }
}

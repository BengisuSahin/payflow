package com.payflow.orders.shared.infrastructure.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Optional;

/**
 * Maps each event type to the Kafka topic it is published on.
 * Configured under payflow.outbox.topics, so the shared outbox never has to
 * import a feature's event classes.
 */
@ConfigurationProperties("payflow.outbox")
public record OutboxTopics(Map<String, String> topics) {

    public OutboxTopics {
        topics = topics == null ? Map.of() : Map.copyOf(topics);
    }

    Optional<String> topicFor(String eventType) {
        return Optional.ofNullable(topics.get(eventType));
    }
}

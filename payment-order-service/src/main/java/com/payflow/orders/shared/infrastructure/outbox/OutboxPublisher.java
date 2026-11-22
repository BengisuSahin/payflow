package com.payflow.orders.shared.infrastructure.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Polls the outbox table and publishes events to Kafka.
 * Delivery is at-least-once: if the service crashes after a send but before the
 * commit, the event is sent again. Consumers must deduplicate on the eventId header.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    static final int BATCH_SIZE = 100;
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final OutboxTopics topics;

    public OutboxPublisher(
            OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            TransactionTemplate transactionTemplate,
            Clock clock,
            OutboxTopics topics
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.topics = topics;
    }

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        transactionTemplate.executeWithoutResult(status -> publishBatch());
    }

    /** Must run inside a transaction: the row locks and publishedAt updates commit together. */
    void publishBatch() {
        List<OutboxEvent> events = repository.lockNextUnpublishedBatch(BATCH_SIZE);

        for (OutboxEvent event : events) {
            if (!send(event)) {
                // Stop at the first failure. Sending later events could deliver a
                // newer event for the same order before this one. Events already
                // sent in this batch are still marked published when we commit.
                return;
            }
            // Dirty checking writes this on commit; no explicit save() needed.
            event.markAsPublished(clock.instant());
        }
    }

    private boolean send(OutboxEvent event) {
        String topic = topics.topicFor(event.getEventType()).orElse(null);
        if (topic == null) {
            log.error("No topic mapped for event type {} (outbox event {})",
                    event.getEventType(), event.getId());
            return false;
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(
                topic,
                event.getAggregateId().toString(),
                event.getPayload()
        );
        record.headers()
                .add("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8))
                .add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));

        try {
            kafkaTemplate.send(record).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while publishing outbox event {}", event.getId());
            return false;
        } catch (ExecutionException | TimeoutException exception) {
            log.warn("Failed to publish outbox event {}; will retry on next poll",
                    event.getId(), exception);
            return false;
        }
    }
}

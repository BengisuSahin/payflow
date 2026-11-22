package com.payflow.orders.adapter.outbox;

import com.payflow.orders.domain.PaymentOrderCreatedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private TransactionTemplate transactionTemplate;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(
                repository, kafkaTemplate, transactionTemplate, Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void publishBatch_sendsKeyedRecordWithHeadersAndMarksPublished() {
        OutboxEvent event = outboxEvent(PaymentOrderCreatedEvent.TYPE);
        when(repository.lockNextUnpublishedBatch(OutboxPublisher.BATCH_SIZE)).thenReturn(List.of(event));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishBatch();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, String>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(sent.capture());

        ProducerRecord<String, String> record = sent.getValue();
        assertThat(record.topic()).isEqualTo("payment-orders.created");
        assertThat(record.key()).isEqualTo(event.getAggregateId().toString());
        assertThat(new String(record.headers().lastHeader("eventId").value(), StandardCharsets.UTF_8))
                .isEqualTo(event.getId().toString());
        assertThat(event.getPublishedAt()).isEqualTo(NOW);
    }

    @Test
    void publishBatch_stopsAtFirstFailureToKeepOrdering() {
        OutboxEvent first = outboxEvent(PaymentOrderCreatedEvent.TYPE);
        OutboxEvent second = outboxEvent(PaymentOrderCreatedEvent.TYPE);
        when(repository.lockNextUnpublishedBatch(OutboxPublisher.BATCH_SIZE)).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        publisher.publishBatch();

        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
        assertThat(first.getPublishedAt()).isNull();
        assertThat(second.getPublishedAt()).isNull();
    }

    @Test
    void publishBatch_withUnknownEventType_sendsNothing() {
        OutboxEvent event = outboxEvent("SomethingElse");
        when(repository.lockNextUnpublishedBatch(OutboxPublisher.BATCH_SIZE)).thenReturn(List.of(event));

        publisher.publishBatch();

        verify(kafkaTemplate, times(0)).send(any(ProducerRecord.class));
        assertThat(event.getPublishedAt()).isNull();
    }

    private static OutboxEvent outboxEvent(String eventType) {
        return new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), eventType, "{}", NOW);
    }
}

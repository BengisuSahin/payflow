package com.payflow.orders.shared.infrastructure.outbox;

import com.payflow.orders.paymentorder.domain.PaymentOrder;
import com.payflow.orders.paymentorder.domain.PaymentOrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JpaEventOutboxTest {

    @Mock
    private OutboxEventRepository repository;

    @Test
    void append_storesEventAsJsonRowKeyedByAggregate() {
        JpaEventOutbox outbox = new JpaEventOutbox(repository, JsonMapper.builder().build());
        PaymentOrder order = PaymentOrder.create(
                "cust-1", new BigDecimal("10.50"), "EUR", null, Instant.parse("2026-01-15T10:00:00Z"));
        PaymentOrderCreatedEvent event = PaymentOrderCreatedEvent.from(order);

        outbox.append(event);

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(saved.capture());

        OutboxEvent row = saved.getValue();
        assertThat(row.getId()).isEqualTo(event.eventId());
        assertThat(row.getAggregateId()).isEqualTo(order.getId());
        assertThat(row.getEventType()).isEqualTo(PaymentOrderCreatedEvent.TYPE);
        assertThat(row.getPayload())
                .contains("\"orderId\":\"" + order.getId() + "\"")
                .contains("\"currency\":\"EUR\"")
                .doesNotContain("aggregateId");
    }
}

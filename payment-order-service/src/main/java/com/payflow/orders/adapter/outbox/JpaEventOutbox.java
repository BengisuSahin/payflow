package com.payflow.orders.adapter.outbox;

import com.payflow.orders.application.EventOutbox;
import com.payflow.orders.domain.DomainEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Stores events as JSON rows in outbox_events; {@link OutboxPublisher} sends them to Kafka. */
@Component
class JpaEventOutbox implements EventOutbox {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    JpaEventOutbox(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** MANDATORY: fails fast if called outside a transaction, which would break atomicity. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(DomainEvent event) {
        repository.save(new OutboxEvent(
                event.eventId(),
                event.aggregateId(),
                event.eventType(),
                toJson(event),
                event.occurredAt()
        ));
    }

    private String toJson(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize " + event.eventType(), exception);
        }
    }
}

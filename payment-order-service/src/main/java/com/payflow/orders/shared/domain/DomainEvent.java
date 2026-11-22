package com.payflow.orders.shared.domain;

import java.time.Instant;
import java.util.UUID;

/** Something that happened to an aggregate and that other services may react to. */
public interface DomainEvent {

    UUID eventId();

    String eventType();

    /** The id of the aggregate the event belongs to; used as the Kafka message key. */
    UUID aggregateId();

    Instant occurredAt();
}

package com.payflow.orders.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentOrderCreatedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID orderId,
        String customerId,
        BigDecimal amount,
        String currency
) implements DomainEvent {

    public static final String TYPE = "PaymentOrderCreated";
    public static final int VERSION = 1;

    @Override
    public UUID aggregateId() {
        return orderId;
    }

    public static PaymentOrderCreatedEvent from(PaymentOrder order) {
        return new PaymentOrderCreatedEvent(
                UUID.randomUUID(),
                TYPE,
                VERSION,
                order.getCreatedAt(),
                order.getId(),
                order.getCustomerId(),
                order.getAmount(),
                order.getCurrency()
        );
    }
}

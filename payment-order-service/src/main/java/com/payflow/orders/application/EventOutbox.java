package com.payflow.orders.application;

import com.payflow.orders.domain.DomainEvent;

/**
 * Port for recording events that must be published after the current
 * transaction commits (transactional outbox). Callers must already be in a
 * transaction, so the event is stored atomically with the state change.
 */
public interface EventOutbox {

    void append(DomainEvent event);
}

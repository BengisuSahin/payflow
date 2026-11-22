package com.payflow.orders.application;

import com.payflow.orders.domain.PaymentOrder;
import com.payflow.orders.domain.PaymentOrderCreatedEvent;
import com.payflow.orders.domain.PaymentOrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentOrderService {

    private final PaymentOrderRepository orderRepository;
    private final EventOutbox eventOutbox;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public PaymentOrderService(
            PaymentOrderRepository orderRepository,
            EventOutbox eventOutbox,
            TransactionTemplate transactionTemplate,
            Clock clock
    ) {
        this.orderRepository = orderRepository;
        this.eventOutbox = eventOutbox;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Creates a payment order, or returns the existing one when the same
     * idempotency key was used before with the same details.
     * Not @Transactional on purpose: a unique-key violation rolls back the insert
     * transaction, and we then look up the winning order in a new one.
     */
    public PaymentOrder createOrder(CreatePaymentOrderCommand command) {
        if (command.idempotencyKey() == null) {
            return transactionTemplate.execute(status -> insertOrderWithEvent(command));
        }

        Optional<PaymentOrder> existing = findPreviousOrder(command);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return transactionTemplate.execute(status -> insertOrderWithEvent(command));
        } catch (DataIntegrityViolationException raceLost) {
            // A concurrent request with the same key committed first.
            return findPreviousOrder(command).orElseThrow(() -> raceLost);
        }
    }

    @Transactional(readOnly = true)
    public PaymentOrder getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new PaymentOrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public Page<PaymentOrder> getOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    /** Saves the order and its outbox event in one transaction (transactional outbox). */
    private PaymentOrder insertOrderWithEvent(CreatePaymentOrderCommand command) {
        PaymentOrder order = PaymentOrder.create(
                command.customerId(),
                command.amount(),
                command.currency(),
                command.idempotencyKey(),
                clock.instant()
        );
        // Flush now so a duplicate idempotency key fails here, not at commit.
        PaymentOrder savedOrder = orderRepository.saveAndFlush(order);

        eventOutbox.append(PaymentOrderCreatedEvent.from(savedOrder));

        return savedOrder;
    }

    private Optional<PaymentOrder> findPreviousOrder(CreatePaymentOrderCommand command) {
        return orderRepository
                .findByCustomerIdAndIdempotencyKey(command.customerId(), command.idempotencyKey())
                .map(order -> {
                    if (!order.hasSameDetails(command.amount(), command.currency())) {
                        throw new IdempotencyKeyReusedException(command.idempotencyKey());
                    }
                    return order;
                });
    }
}

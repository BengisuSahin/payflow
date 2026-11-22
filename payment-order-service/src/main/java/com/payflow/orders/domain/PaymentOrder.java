package com.payflow.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_orders_customer_idempotency_key",
                columnNames = {"customer_id", "idempotency_key"}
        )
)
public class PaymentOrder {

    @Id
    private UUID id;

    // Optimistic locking. A null version also tells Spring Data the entity is new,
    // so save() calls persist() instead of merge() and skips an extra SELECT.
    @Version
    private Long version;

    @Column(nullable = false, length = 64)
    private String customerId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentOrderStatus status;

    @Column(length = 255)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant createdAt;

    protected PaymentOrder() {
    }

    private PaymentOrder(
            UUID id,
            String customerId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            Instant createdAt
    ) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentOrderStatus.CREATED;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public static PaymentOrder create(
            String customerId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            Instant createdAt
    ) {
        return new PaymentOrder(
                UUID.randomUUID(), customerId, amount, currency, idempotencyKey, createdAt
        );
    }

    /** True when a retried request carries the same payment details as this order. */
    public boolean hasSameDetails(BigDecimal otherAmount, String otherCurrency) {
        return amount.compareTo(otherAmount) == 0 && currency.equals(otherCurrency);
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentOrderStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

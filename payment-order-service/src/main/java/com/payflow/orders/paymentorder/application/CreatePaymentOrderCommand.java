package com.payflow.orders.paymentorder.application;

import java.math.BigDecimal;

/**
 * Input to the service layer. Keeps the service independent of the web DTOs.
 * idempotencyKey is null when the client did not send one.
 */
public record CreatePaymentOrderCommand(
        String customerId,
        BigDecimal amount,
        String currency,
        String idempotencyKey
) {}

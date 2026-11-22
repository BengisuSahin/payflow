package com.payflow.orders.paymentorder.infrastructure.web;

import com.payflow.orders.paymentorder.domain.PaymentOrder;
import com.payflow.orders.paymentorder.domain.PaymentOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentOrderResponse(
        UUID orderId,
        String customerId,
        BigDecimal amount,
        String currency,
        PaymentOrderStatus status,
        Instant createdAt
) {

    public static PaymentOrderResponse from(PaymentOrder order) {
        return new PaymentOrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}

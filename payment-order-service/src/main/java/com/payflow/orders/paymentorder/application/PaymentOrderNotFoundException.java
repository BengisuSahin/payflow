package com.payflow.orders.paymentorder.application;

import java.util.UUID;

public class PaymentOrderNotFoundException extends RuntimeException {

    public PaymentOrderNotFoundException(UUID orderId) {
        super("Payment order " + orderId + " not found");
    }
}

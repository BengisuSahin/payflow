package com.payflow.orders.paymentorder.application;

/** The same idempotency key was sent again with different payment details. */
public class IdempotencyKeyReusedException extends RuntimeException {

    public IdempotencyKeyReusedException(String idempotencyKey) {
        super("Idempotency key '" + idempotencyKey
                + "' was already used for a payment order with different details");
    }
}

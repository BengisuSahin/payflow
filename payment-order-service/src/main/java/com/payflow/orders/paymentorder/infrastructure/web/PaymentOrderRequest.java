package com.payflow.orders.paymentorder.infrastructure.web;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentOrderRequest(

        @NotBlank
        @Size(max = 64)
        String customerId,

        // Matches the numeric(19, 4) column, so values are never silently rounded.
        @NotNull
        @Positive
        @Digits(integer = 15, fraction = 4)
        BigDecimal amount,

        @NotNull
        @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO 4217 code, e.g. EUR")
        String currency

) {}

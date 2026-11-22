package com.payflow.orders.adapter.web;

import com.payflow.orders.application.CreatePaymentOrderCommand;
import com.payflow.orders.application.PaymentOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payment-orders")
public class PaymentOrderController {

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final PaymentOrderService service;

    public PaymentOrderController(PaymentOrderService service) {
        this.service = service;
    }

    /**
     * A retry with the same Idempotency-Key and body returns the original order
     * (same status code, like Stripe) instead of creating a second payment.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentOrderResponse createOrder(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 1, max = 255) String idempotencyKey,
            @Valid @RequestBody PaymentOrderRequest request
    ) {
        CreatePaymentOrderCommand command = new CreatePaymentOrderCommand(
                request.customerId(),
                request.amount(),
                request.currency(),
                idempotencyKey
        );
        return PaymentOrderResponse.from(service.createOrder(command));
    }

    @GetMapping("/{orderId}")
    public PaymentOrderResponse getOrder(@PathVariable UUID orderId) {
        return PaymentOrderResponse.from(service.getOrder(orderId));
    }

    @GetMapping
    public PagedModel<PaymentOrderResponse> getOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return new PagedModel<>(service.getOrders(pageable).map(PaymentOrderResponse::from));
    }
}

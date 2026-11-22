package com.payflow.orders.adapter.web;

import com.payflow.orders.domain.PaymentOrder;
import com.payflow.orders.application.CreatePaymentOrderCommand;
import com.payflow.orders.application.IdempotencyKeyReusedException;
import com.payflow.orders.application.PaymentOrderNotFoundException;
import com.payflow.orders.application.PaymentOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentOrderController.class)
class PaymentOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentOrderService service;

    @Test
    void createOrder_returns201AndPassesIdempotencyKey() throws Exception {
        PaymentOrder order = PaymentOrder.create(
                "cust-1", new BigDecimal("10.50"), "EUR", "key-1", Instant.parse("2026-01-15T10:00:00Z"));
        when(service.createOrder(any())).thenReturn(order);

        mockMvc.perform(post("/api/payment-orders")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("10.50", "EUR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(order.getId().toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));

        ArgumentCaptor<CreatePaymentOrderCommand> command = ArgumentCaptor.forClass(CreatePaymentOrderCommand.class);
        verify(service).createOrder(command.capture());
        assertThat(command.getValue().idempotencyKey()).isEqualTo("key-1");
    }

    @Test
    void createOrder_withInvalidCurrency_returns400Problem() throws Exception {
        mockMvc.perform(post("/api/payment-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("10.50", "EURO")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(service);
    }

    @Test
    void createOrder_withMoreThanFourDecimals_returns400() throws Exception {
        mockMvc.perform(post("/api/payment-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("10.12345", "EUR")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void createOrder_withReusedIdempotencyKey_returns422() throws Exception {
        when(service.createOrder(any())).thenThrow(new IdempotencyKeyReusedException("key-1"));

        mockMvc.perform(post("/api/payment-orders")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("10.50", "EUR")))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void getOrder_whenMissing_returns404Problem() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getOrder(id)).thenThrow(new PaymentOrderNotFoundException(id));

        mockMvc.perform(get("/api/payment-orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Payment order " + id + " not found"));
    }

    private static String body(String amount, String currency) {
        return """
                {"customerId": "cust-1", "amount": %s, "currency": "%s"}
                """.formatted(amount, currency);
    }
}

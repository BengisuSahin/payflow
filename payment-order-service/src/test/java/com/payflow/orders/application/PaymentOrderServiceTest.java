package com.payflow.orders.application;

import com.payflow.orders.domain.PaymentOrder;
import com.payflow.orders.domain.PaymentOrderCreatedEvent;
import com.payflow.orders.domain.PaymentOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final String KEY = "key-123";

    @Mock
    private PaymentOrderRepository orderRepository;

    @Mock
    private EventOutbox eventOutbox;

    @Mock
    private TransactionTemplate transactionTemplate;

    private PaymentOrderService service;

    @BeforeEach
    void setUp() {
        service = new PaymentOrderService(
                orderRepository,
                eventOutbox,
                transactionTemplate,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createOrder_savesOrderAndAppendsCreatedEvent() {
        runTransactionsInline();

        PaymentOrder order = service.createOrder(command("10.50", "EUR", null));

        ArgumentCaptor<PaymentOrderCreatedEvent> event = ArgumentCaptor.forClass(PaymentOrderCreatedEvent.class);
        verify(eventOutbox).append(event.capture());

        assertThat(order.getCreatedAt()).isEqualTo(NOW);
        assertThat(event.getValue().orderId()).isEqualTo(order.getId());
        assertThat(event.getValue().occurredAt()).isEqualTo(order.getCreatedAt());
        assertThat(event.getValue().amount()).isEqualByComparingTo("10.50");
    }

    @Test
    void createOrder_withRepeatedKeyAndSameDetails_returnsExistingOrder() {
        PaymentOrder existing = existingOrder("10.50", "EUR");
        when(orderRepository.findByCustomerIdAndIdempotencyKey("cust-1", KEY))
                .thenReturn(Optional.of(existing));

        // 10.5 and 10.50 are the same amount; compareTo ignores scale.
        PaymentOrder result = service.createOrder(command("10.5", "EUR", KEY));

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(transactionTemplate, eventOutbox);
    }

    @Test
    void createOrder_withRepeatedKeyAndDifferentAmount_isRejected() {
        when(orderRepository.findByCustomerIdAndIdempotencyKey("cust-1", KEY))
                .thenReturn(Optional.of(existingOrder("10.50", "EUR")));

        assertThatThrownBy(() -> service.createOrder(command("99.00", "EUR", KEY)))
                .isInstanceOf(IdempotencyKeyReusedException.class);
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void createOrder_whenConcurrentRequestWinsRace_returnsWinningOrder() {
        PaymentOrder winner = existingOrder("10.50", "EUR");
        when(orderRepository.findByCustomerIdAndIdempotencyKey("cust-1", KEY))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(transactionTemplate.execute(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        PaymentOrder result = service.createOrder(command("10.50", "EUR", KEY));

        assertThat(result).isSameAs(winner);
    }

    @Test
    void getOrder_whenMissing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrder(id))
                .isInstanceOf(PaymentOrderNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    private void runTransactionsInline() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static CreatePaymentOrderCommand command(String amount, String currency, String key) {
        return new CreatePaymentOrderCommand("cust-1", new BigDecimal(amount), currency, key);
    }

    private static PaymentOrder existingOrder(String amount, String currency) {
        return PaymentOrder.create("cust-1", new BigDecimal(amount), currency, KEY, NOW);
    }
}

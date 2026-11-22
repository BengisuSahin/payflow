package com.payflow.orders;

import com.payflow.orders.paymentorder.domain.PaymentOrderCreatedEvent;
import com.payflow.orders.shared.infrastructure.outbox.OutboxTopics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentOrderServiceApplicationTests {

	@Autowired
	private OutboxTopics outboxTopics;

	@Test
	void contextLoads() {
	}

	@Test
	void everyPublishedEventTypeHasATopic() {
		assertThat(outboxTopics.topics())
				.containsEntry(PaymentOrderCreatedEvent.TYPE, "payment-orders.created");
	}

}

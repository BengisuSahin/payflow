package com.payflow.orders.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {

    Optional<PaymentOrder> findByCustomerIdAndIdempotencyKey(String customerId, String idempotencyKey);
}

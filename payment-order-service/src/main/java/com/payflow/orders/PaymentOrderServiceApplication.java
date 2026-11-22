package com.payflow.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaymentOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentOrderServiceApplication.class, args);
    }

}

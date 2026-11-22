package com.payflow.orders.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Injected wherever the code needs "now", so tests can use a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

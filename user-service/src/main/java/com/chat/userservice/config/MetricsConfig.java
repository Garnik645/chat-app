package com.chat.userservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    /**
     * Custom business metric: counts total successful user registrations.
     * Exposed at /actuator/prometheus as users_registered_total.
     */
    @Bean
    public Counter usersRegisteredCounter(MeterRegistry meterRegistry) {
        return Counter.builder("users_registered_total")
                .description("Total number of successfully registered users")
                .register(meterRegistry);
    }
}

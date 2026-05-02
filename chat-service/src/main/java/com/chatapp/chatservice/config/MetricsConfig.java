package com.chatapp.chatservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter messagesSentCounter(MeterRegistry registry) {
        return Counter.builder("messages_sent_total")
                .description("Total number of messages sent")
                .register(registry);
    }

    @Bean
    public Counter messagesRejectedCounter(MeterRegistry registry) {
        return Counter.builder("messages_rejected_total")
                .description("Total number of messages rejected by moderation")
                .register(registry);
    }

    @Bean
    public AtomicInteger activeWebSocketConnections() {
        return new AtomicInteger(0);
    }

    @Bean
    public Gauge websocketConnectionsActiveGauge(MeterRegistry registry,
                                                  AtomicInteger activeWebSocketConnections) {
        return Gauge.builder("websocket_connections_active", activeWebSocketConnections, AtomicInteger::get)
                .description("Number of currently active WebSocket connections")
                .register(registry);
    }
}

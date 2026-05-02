package com.example.roomservice.config;

import com.example.roomservice.repository.RoomMemberRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    /**
     * rooms_created_total — counter incremented on every room creation.
     */
    @Bean
    public Counter roomsCreatedCounter(MeterRegistry registry) {
        return Counter.builder("rooms_created_total")
                .description("Total number of chat rooms created")
                .register(registry);
    }

    /**
     * room_members_total — gauge reflecting the current total number of memberships.
     */
    @Bean
    public Gauge roomMembersGauge(MeterRegistry registry, RoomMemberRepository roomMemberRepository) {
        return Gauge.builder("room_members_total", roomMemberRepository, RoomMemberRepository::count)
                .description("Total number of room memberships")
                .register(registry);
    }
}

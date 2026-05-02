package com.example.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic route override for the WebSocket proxy.
 * <p>
 * Spring Cloud Gateway requires the target URI to use the {@code ws://}
 * (or {@code wss://}) scheme in order to trigger the WebSocket upgrade
 * handshake. The {@code CHAT_SERVICE_URL} environment variable is provided
 * as {@code http://…} so we convert it here before handing it to the router.
 * <p>
 * This bean overrides the {@code chat-service-websocket} route that is
 * declared in {@code application.yml} with an identical predicate but the
 * correct {@code ws://} URI so the gateway performs the full WebSocket proxy.
 */
@Configuration
public class WebSocketRouteConfig {

    @Value("${services.chat-service}")
    private String chatServiceUrl;

    @Bean
    public RouteLocator webSocketRoute(RouteLocatorBuilder builder) {
        // Derive the ws:// base URL from the http:// service URL.
        String wsUri = chatServiceUrl
                .replaceFirst("^http://",  "ws://")
                .replaceFirst("^https://", "wss://");

        return builder.routes()
                .route("chat-service-websocket-ws", r -> r
                        .path("/ws/**")
                        .uri(wsUri))
                .build();
    }
}

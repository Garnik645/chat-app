package com.example.apigateway.filter;

import com.example.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Gateway filter that validates the JWT on every request reaching a protected route.
 * <p>
 * When the token is valid the filter mutates the downstream request by adding:
 * <ul>
 *   <li>{@code X-User-Id}   — the subject claim of the token</li>
 *   <li>{@code X-User-Role} — the {@code role} claim of the token</li>
 * </ul>
 * When the token is missing or invalid the filter short-circuits the exchange
 * and returns {@code 401 Unauthorized} without forwarding the request.
 * <p>
 * Public routes ({@code /api/users/register} and {@code /api/users/login}) are
 * configured in {@code application.yml} without this filter, so they are never
 * passed through here.
 */
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    static final String HEADER_USER_ID   = "X-User-Id";
    static final String HEADER_USER_ROLE = "X-User-Role";
    static final String HEADER_USERNAME  = "X-Username";
    static final String CLAIM_ROLE       = "role";
    static final String CLAIM_USERNAME   = "username";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            String token      = jwtUtil.extractRawToken(authHeader);

            // WebSocket upgrade: token is passed as a query parameter because
            // browsers cannot set custom headers on WebSocket connections.
            if (token == null || token.isBlank()) {
                token = exchange.getRequest().getQueryParams().getFirst("token");
            }

            Optional<Claims> claimsOpt = jwtUtil.validateAndExtract(token);

            if (claimsOpt.isEmpty()) {
                log.debug("Rejecting unauthenticated request to {}",
                        exchange.getRequest().getPath());
                return unauthorized(exchange);
            }

            Claims claims   = claimsOpt.get();
            String userId   = claims.getSubject();
            String role     = claims.get(CLAIM_ROLE, String.class);
            String username = claims.get(CLAIM_USERNAME, String.class);

            // Mutate the downstream request to carry the authenticated identity.
            // Downstream services MUST read from these headers, never from the JWT.
            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header(HEADER_USER_ID,   userId   != null ? userId   : "")
                    .header(HEADER_USER_ROLE, role     != null ? role     : "")
                    .header(HEADER_USERNAME,  username != null ? username : "")
                    // Strip the original Authorization header so downstream services
                    // cannot accidentally re-validate or misuse the raw JWT.
                    .headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION))
                    .build();

            log.debug("Authenticated request from userId={} role={} to {}",
                    userId, role, exchange.getRequest().getPath());

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    public static class Config {
        // No configuration fields needed for this filter.
    }
}

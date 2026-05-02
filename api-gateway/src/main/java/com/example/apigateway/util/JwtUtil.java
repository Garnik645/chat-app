package com.example.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Stateless helper that validates JWT tokens and extracts claims.
 * <p>
 * Tokens are signed with an HMAC-SHA key whose raw bytes come from
 * the {@code JWT_SECRET} environment variable. The same secret must
 * be used by the User Service when issuing tokens.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // Derive a SecretKey from the raw secret bytes.
        // The key must be at least 256 bits (32 bytes) for HS256.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates the token and returns its claims, or empty if the token is
     * missing, expired, or tampered.
     */
    public Optional<Claims> validateAndExtract(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Strips the {@code Bearer } prefix if present and returns the raw token.
     */
    public String extractRawToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return authorizationHeader;
    }
}

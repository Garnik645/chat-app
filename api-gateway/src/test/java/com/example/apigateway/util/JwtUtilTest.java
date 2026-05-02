package com.example.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long!!";

    private JwtUtil jwtUtil;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String buildToken(String subject, String role, boolean expired) {
        long now = System.currentTimeMillis();
        long expiry = expired ? now - 1000 : now + 60_000;
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(expiry))
                .signWith(signingKey)
                .compact();
    }

    @Test
    void validToken_returnsClaimsWithSubjectAndRole() {
        String token = buildToken("user-123", "ROLE_USER", false);

        Optional<Claims> result = jwtUtil.validateAndExtract(token);

        assertThat(result).isPresent();
        assertThat(result.get().getSubject()).isEqualTo("user-123");
        assertThat(result.get().get("role", String.class)).isEqualTo("ROLE_USER");
    }

    @Test
    void expiredToken_returnsEmpty() {
        String token = buildToken("user-123", "ROLE_USER", true);

        assertThat(jwtUtil.validateAndExtract(token)).isEmpty();
    }

    @Test
    void tamperedToken_returnsEmpty() {
        String token = buildToken("user-123", "ROLE_USER", false);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThat(jwtUtil.validateAndExtract(tampered)).isEmpty();
    }

    @Test
    void nullToken_returnsEmpty() {
        assertThat(jwtUtil.validateAndExtract(null)).isEmpty();
    }

    @Test
    void blankToken_returnsEmpty() {
        assertThat(jwtUtil.validateAndExtract("   ")).isEmpty();
    }

    @Test
    void extractRawToken_stripsBearerPrefix() {
        assertThat(jwtUtil.extractRawToken("Bearer abc.def.ghi")).isEqualTo("abc.def.ghi");
    }

    @Test
    void extractRawToken_noPrefixReturnsAsIs() {
        assertThat(jwtUtil.extractRawToken("abc.def.ghi")).isEqualTo("abc.def.ghi");
    }

    @Test
    void extractRawToken_nullReturnsNull() {
        assertThat(jwtUtil.extractRawToken(null)).isNull();
    }
}

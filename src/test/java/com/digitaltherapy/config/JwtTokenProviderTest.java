package com.digitaltherapy.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secret",
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256");
        ReflectionTestUtils.setField(jwtTokenProvider, "expiration", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpiration", 86400000L);
    }

    @Test
    void generateToken_ReturnsNonNullToken() {
        // when
        String token = jwtTokenProvider.generateToken("user@example.com");

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
    }

    @Test
    void generateRefreshToken_ReturnsNonNullToken() {
        // when
        String refreshToken = jwtTokenProvider.generateRefreshToken("user@example.com");

        // then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotBlank();
    }

    @Test
    void getEmailFromToken_ReturnsCorrectEmail() {
        // given
        String email = "test@digitaltherapy.com";
        String token = jwtTokenProvider.generateToken(email);

        // when
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // then
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // given
        String token = jwtTokenProvider.generateToken("user@example.com");

        // when
        boolean valid = jwtTokenProvider.validateToken(token);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        // given - create a provider with 0ms expiration so the token expires immediately
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(expiredProvider, "secret",
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256");
        ReflectionTestUtils.setField(expiredProvider, "expiration", -1000L);
        ReflectionTestUtils.setField(expiredProvider, "refreshExpiration", 86400000L);

        String token = expiredProvider.generateToken("user@example.com");

        // when - validate with the same signing key
        boolean valid = expiredProvider.validateToken(token);

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void validateToken_MalformedToken_ReturnsFalse() {
        // when
        boolean valid = jwtTokenProvider.validateToken("this.is.not.a.valid.jwt");

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void validateToken_NullToken_ReturnsFalse() {
        // when
        boolean valid = jwtTokenProvider.validateToken(null);

        // then
        assertThat(valid).isFalse();
    }
}

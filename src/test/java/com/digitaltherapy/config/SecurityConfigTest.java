package com.digitaltherapy.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    @DisplayName("passwordEncoder returns BCryptPasswordEncoder")
    void passwordEncoder_ReturnsBCrypt() {
        JwtAuthenticationFilter mockFilter = null;
        // Use reflection or direct construction. SecurityConfig needs a filter but for
        // testing passwordEncoder we can work around it.
        SecurityConfig config = new SecurityConfig(mockFilter);
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("passwordEncoder can encode and verify passwords")
    void passwordEncoder_EncodesAndVerifies() {
        SecurityConfig config = new SecurityConfig(null);
        PasswordEncoder encoder = config.passwordEncoder();

        String rawPassword = "securePassword123";
        String encoded = encoder.encode(rawPassword);

        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
        assertThat(encoder.matches("wrongPassword", encoded)).isFalse();
    }
}

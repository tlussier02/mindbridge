package com.digitaltherapy.config;

import com.digitaltherapy.entity.User;
import com.digitaltherapy.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_ValidToken_UserFound_SetsAuthentication() throws Exception {
        // given
        String token = "valid-token";
        String email = "user@test.com";
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hashed")
                .name("Test User")
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(token)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(user);
        assertThat(authentication.getCredentials()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ValidToken_UserNotFound_NoAuthentication() throws Exception {
        // given
        String token = "valid-token";
        String email = "unknown@test.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(token)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_InvalidToken_NoAuthentication() throws Exception {
        // given
        String token = "invalid-token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void doFilter_NoAuthHeader_NoAuthentication() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    void doFilter_EmptyBearerToken_NoAuthentication() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    void shouldNotFilter_AuthPath_ReturnsTrue() {
        // given
        when(request.getServletPath()).thenReturn("/auth/login");

        // when
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotFilter_SwaggerPath_ReturnsTrue() {
        // given
        when(request.getServletPath()).thenReturn("/swagger-ui/index.html");

        // when
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotFilter_SessionsPath_ReturnsFalse() {
        // given
        when(request.getServletPath()).thenReturn("/sessions");

        // when
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // then
        assertThat(result).isFalse();
    }
}

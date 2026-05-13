package com.digitaltherapy.service.impl;

import com.digitaltherapy.config.JwtTokenProvider;
import com.digitaltherapy.dto.AuthResponse;
import com.digitaltherapy.dto.LoginRequest;
import com.digitaltherapy.dto.RegisterRequest;
import com.digitaltherapy.entity.User;
import com.digitaltherapy.exception.AuthenticationException;
import com.digitaltherapy.exception.DuplicateResourceException;
import com.digitaltherapy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_NAME = "Test User";
    private static final String ENCODED_PASSWORD = "encoded_password";
    private static final String ACCESS_TOKEN = "access.token.value";
    private static final String REFRESH_TOKEN = "refresh.token.value";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .email(TEST_EMAIL)
                .passwordHash(ENCODED_PASSWORD)
                .name(TEST_NAME)
                .onboardingComplete(false)
                .streakDays(0)
                .build();
    }

    @Test
    @DisplayName("register - success - returns tokens and userId")
    void register_Success() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .name(TEST_NAME)
                .build();

        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(TEST_EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtTokenProvider.generateRefreshToken(TEST_EMAIL)).thenReturn(REFRESH_TOKEN);
        when(jwtTokenProvider.getExpirationFromToken(ACCESS_TOKEN)).thenReturn(expiration);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(response.getName()).isEqualTo(TEST_NAME);
        assertThat(response.getExpiresIn()).isGreaterThan(0);

        verify(userRepository).existsByEmail(TEST_EMAIL);
        verify(passwordEncoder).encode(TEST_PASSWORD);
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateToken(TEST_EMAIL);
        verify(jwtTokenProvider).generateRefreshToken(TEST_EMAIL);
    }

    @Test
    @DisplayName("register - duplicate email - throws DuplicateResourceException")
    void register_DuplicateEmail_ThrowsDuplicateResourceException() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .name(TEST_NAME)
                .build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(TEST_EMAIL);

        verify(userRepository).existsByEmail(TEST_EMAIL);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login - success - returns tokens")
    void login_Success() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtTokenProvider.generateToken(TEST_EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtTokenProvider.generateRefreshToken(TEST_EMAIL)).thenReturn(REFRESH_TOKEN);
        when(jwtTokenProvider.getExpirationFromToken(ACCESS_TOKEN)).thenReturn(expiration);

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(response.getName()).isEqualTo(TEST_NAME);

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
    }

    @Test
    @DisplayName("login - wrong password - throws AuthenticationException")
    void login_WrongPassword_ThrowsAuthenticationException() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email(TEST_EMAIL)
                .password("wrong_password")
                .build();

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_password", ENCODED_PASSWORD)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid email or password");

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches("wrong_password", ENCODED_PASSWORD);
        verify(jwtTokenProvider, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("login - user not found - throws AuthenticationException")
    void login_UserNotFound_ThrowsAuthenticationException() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid email or password");

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("refreshToken - valid token - returns new tokens")
    void refreshToken_ValidToken_ReturnsNewTokens() {
        // Arrange
        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        when(jwtTokenProvider.validateToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(TEST_EMAIL)).thenReturn("new.access.token");
        when(jwtTokenProvider.generateRefreshToken(TEST_EMAIL)).thenReturn("new.refresh.token");
        when(jwtTokenProvider.getExpirationFromToken("new.access.token")).thenReturn(expiration);

        // Act
        AuthResponse response = authService.refreshToken(REFRESH_TOKEN);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("new.refresh.token");
        assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(response.getName()).isEqualTo(TEST_NAME);

        verify(jwtTokenProvider).validateToken(REFRESH_TOKEN);
        verify(jwtTokenProvider).getEmailFromToken(REFRESH_TOKEN);
        verify(userRepository).findByEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("refreshToken - invalid token - throws AuthenticationException")
    void refreshToken_InvalidToken_ThrowsAuthenticationException() {
        // Arrange
        String invalidToken = "invalid.refresh.token";

        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid or expired refresh token");

        verify(jwtTokenProvider).validateToken(invalidToken);
        verify(jwtTokenProvider, never()).getEmailFromToken(anyString());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("logout - valid token - blacklists the token without exception")
    void logout_ValidToken() {
        // Arrange
        when(jwtTokenProvider.validateToken(ACCESS_TOKEN)).thenReturn(true);

        // Act - should not throw any exception
        authService.logout(ACCESS_TOKEN);

        // Assert
        verify(jwtTokenProvider).validateToken(ACCESS_TOKEN);
    }
}

package com.digitaltherapy.service.impl;

import com.digitaltherapy.config.JwtTokenProvider;
import com.digitaltherapy.dto.AuthResponse;
import com.digitaltherapy.dto.LoginRequest;
import com.digitaltherapy.dto.RegisterRequest;
import com.digitaltherapy.entity.User;
import com.digitaltherapy.exception.AuthenticationException;
import com.digitaltherapy.exception.DuplicateResourceException;
import com.digitaltherapy.repository.UserRepository;
import com.digitaltherapy.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private final Set<String> blacklistedTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email " + request.getEmail() + " already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .onboardingComplete(false)
                .streakDays(0)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {}", savedUser.getId());

        return buildAuthResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        log.info("User logged in successfully: {}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("User not found for refresh token"));

        log.info("Token refreshed successfully for user: {}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    public void logout(String accessToken) {
        log.info("Logging out user");

        if (jwtTokenProvider.validateToken(accessToken)) {
            blacklistedTokens.add(accessToken);
            log.info("Token blacklisted successfully");
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        Date expiration = jwtTokenProvider.getExpirationFromToken(accessToken);
        long expiresIn = (expiration.getTime() - System.currentTimeMillis()) / 1000;

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .userId(user.getId())
                .name(user.getName())
                .build();
    }
}

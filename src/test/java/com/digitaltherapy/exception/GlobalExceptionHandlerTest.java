package com.digitaltherapy.exception;

import com.digitaltherapy.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleResourceNotFound_Returns404() {
        // given
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("User not found");
        assertThat(response.getBody().getError().getTimestamp()).isNotNull();
    }

    @Test
    void handleAuthenticationException_Returns401() {
        // given
        AuthenticationException ex = new AuthenticationException("Invalid credentials");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleAuthentication(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("AUTHENTICATION_ERROR");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Invalid credentials");
        assertThat(response.getBody().getError().getTimestamp()).isNotNull();
    }

    @Test
    void handleDuplicateResource_Returns409() {
        // given
        DuplicateResourceException ex = new DuplicateResourceException("Email already registered");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResource(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Email already registered");
        assertThat(response.getBody().getError().getTimestamp()).isNotNull();
    }

    @Test
    void handleValidationException_Returns422() {
        // given
        ValidationException ex = new ValidationException("Mood value must be between 1 and 10");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Mood value must be between 1 and 10");
        assertThat(response.getBody().getError().getTimestamp()).isNotNull();
    }

    @Test
    void handleGenericException_Returns500() {
        // given
        Exception ex = new Exception("Something went wrong");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getError().getTimestamp()).isNotNull();
    }
}

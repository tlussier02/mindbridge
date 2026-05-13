package com.digitaltherapy.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionTest {

    @Test
    @DisplayName("Constructor sets statusCode, errorCode, and message correctly")
    void constructor_SetsAllFields() {
        ApiException exception = new ApiException(404, "NOT_FOUND", "Resource not found");

        assertThat(exception.getStatusCode()).isEqualTo(404);
        assertThat(exception.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("Resource not found");
    }

    @Test
    @DisplayName("ApiException is a RuntimeException")
    void apiException_IsRuntimeException() {
        ApiException exception = new ApiException(500, "INTERNAL_ERROR", "Server error");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("ApiException with zero statusCode and custom errorCode")
    void apiException_ConnectionError() {
        ApiException exception = new ApiException(0, "CONNECTION_ERROR", "Cannot connect");

        assertThat(exception.getStatusCode()).isEqualTo(0);
        assertThat(exception.getErrorCode()).isEqualTo("CONNECTION_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Cannot connect");
    }
}

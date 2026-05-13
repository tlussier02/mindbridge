package com.digitaltherapy.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiClientTest {

    private ApiClient apiClient;

    @BeforeEach
    void setUp() {
        // Use a port that nothing is listening on
        apiClient = new ApiClient("http://localhost:19999");
    }

    // =========================================================================
    // State management tests
    // =========================================================================

    @Test
    @DisplayName("isAuthenticated returns false when no token is set")
    void isAuthenticated_NoToken_ReturnsFalse() {
        assertThat(apiClient.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("isAuthenticated returns true after setAuthState")
    void isAuthenticated_AfterSetAuthState_ReturnsTrue() {
        apiClient.setAuthState("access-token", "refresh-token", 3600L);
        assertThat(apiClient.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("getAccessToken returns the set token")
    void getAccessToken_AfterSetAuthState_ReturnsToken() {
        apiClient.setAuthState("my-access-token", "my-refresh-token", 3600L);
        assertThat(apiClient.getAccessToken()).isEqualTo("my-access-token");
    }

    @Test
    @DisplayName("getAccessToken returns null when no token is set")
    void getAccessToken_NoToken_ReturnsNull() {
        assertThat(apiClient.getAccessToken()).isNull();
    }

    @Test
    @DisplayName("clearAuthState removes tokens and resets authentication")
    void clearAuthState_RemovesTokens() {
        apiClient.setAuthState("access-token", "refresh-token", 3600L);
        apiClient.clearAuthState();
        assertThat(apiClient.isAuthenticated()).isFalse();
        assertThat(apiClient.getAccessToken()).isNull();
    }

    @Test
    @DisplayName("setAuthState with null expiresIn does not throw")
    void setAuthState_NullExpiresIn_DoesNotThrow() {
        apiClient.setAuthState("token", "refresh", null);
        assertThat(apiClient.isAuthenticated()).isTrue();
    }

    // =========================================================================
    // Authentication guard tests — calls without token
    // =========================================================================

    @Test
    @DisplayName("get with Class throws ApiException when not authenticated")
    void get_NotAuthenticated_ThrowsApiException() {
        assertThatThrownBy(() -> apiClient.get("/test", String.class))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Please log in first")
                .extracting(e -> ((ApiException) e).getStatusCode())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("get with ParameterizedTypeReference throws ApiException when not authenticated")
    void getWithTypeRef_NotAuthenticated_ThrowsApiException() {
        assertThatThrownBy(() -> apiClient.get("/test", new ParameterizedTypeReference<Map<String, Object>>() {}))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Please log in first");
    }

    @Test
    @DisplayName("post with Class throws ApiException when not authenticated")
    void post_NotAuthenticated_ThrowsApiException() {
        assertThatThrownBy(() -> apiClient.post("/test", Map.of("key", "val"), String.class))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Please log in first");
    }

    @Test
    @DisplayName("post with ParameterizedTypeReference throws ApiException when not authenticated")
    void postWithTypeRef_NotAuthenticated_ThrowsApiException() {
        assertThatThrownBy(() -> apiClient.post("/test", null, new ParameterizedTypeReference<Map<String, Object>>() {}))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Please log in first");
    }

    @Test
    @DisplayName("postVoid throws ApiException when not authenticated")
    void postVoid_NotAuthenticated_ThrowsApiException() {
        assertThatThrownBy(() -> apiClient.postVoid("/test", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Please log in first");
    }

    // =========================================================================
    // Connection error tests — server not running
    // =========================================================================

    @Test
    @DisplayName("postPublic throws ApiException with CONNECTION_ERROR when server not running")
    void postPublic_ConnectionRefused_ThrowsConnectionError() {
        assertThatThrownBy(() -> apiClient.postPublic("/test", Map.of("key", "val"), String.class))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiEx = (ApiException) e;
                    assertThat(apiEx.getErrorCode()).isEqualTo("CONNECTION_ERROR");
                    assertThat(apiEx.getStatusCode()).isEqualTo(0);
                    assertThat(apiEx.getMessage()).contains("Cannot connect to server");
                });
    }

    @Test
    @DisplayName("postPublic with null body throws ApiException with CONNECTION_ERROR")
    void postPublic_NullBody_ConnectionRefused_ThrowsConnectionError() {
        assertThatThrownBy(() -> apiClient.postPublic("/test", null, String.class))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiEx = (ApiException) e;
                    assertThat(apiEx.getErrorCode()).isEqualTo("CONNECTION_ERROR");
                });
    }

    @Test
    @DisplayName("getPublic with Class throws ApiException with CONNECTION_ERROR when server not running")
    void getPublic_ConnectionRefused_ThrowsConnectionError() {
        assertThatThrownBy(() -> apiClient.getPublic("/test", String.class))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiEx = (ApiException) e;
                    assertThat(apiEx.getErrorCode()).isEqualTo("CONNECTION_ERROR");
                    assertThat(apiEx.getStatusCode()).isEqualTo(0);
                });
    }

    @Test
    @DisplayName("getPublic with ParameterizedTypeReference throws CONNECTION_ERROR when server not running")
    void getPublicWithTypeRef_ConnectionRefused_ThrowsConnectionError() {
        assertThatThrownBy(() -> apiClient.getPublic("/test", new ParameterizedTypeReference<Map<String, Object>>() {}))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiEx = (ApiException) e;
                    assertThat(apiEx.getErrorCode()).isEqualTo("CONNECTION_ERROR");
                });
    }

    // =========================================================================
    // Authenticated calls with connection error
    // =========================================================================

    @Test
    @DisplayName("get with valid token throws CONNECTION_ERROR when server not running")
    void get_ValidToken_ConnectionRefused_ThrowsConnectionError() {
        // Set token with long expiry so it doesn't trigger refresh
        apiClient.setAuthState("valid-token", "refresh-token", null);
        assertThatThrownBy(() -> apiClient.get("/test", String.class))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiEx = (ApiException) e;
                    assertThat(apiEx.getErrorCode()).isEqualTo("CONNECTION_ERROR");
                });
    }

    @Test
    @DisplayName("post with valid token throws CONNECTION_ERROR when server not running")
    void post_ValidToken_ConnectionRefused_ThrowsConnectionError() {
        apiClient.setAuthState("valid-token", "refresh-token", null);
        assertThatThrownBy(() -> apiClient.post("/test", Map.of("data", "value"), String.class))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiEx = (ApiException) e;
                    assertThat(apiEx.getErrorCode()).isEqualTo("CONNECTION_ERROR");
                });
    }

    @Test
    @DisplayName("post with null body and valid token throws CONNECTION_ERROR")
    void post_NullBody_ValidToken_ConnectionRefused_ThrowsConnectionError() {
        apiClient.setAuthState("valid-token", "refresh-token", null);
        assertThatThrownBy(() -> apiClient.post("/test", null, String.class))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo("CONNECTION_ERROR"));
    }

    @Test
    @DisplayName("postVoid with valid token throws CONNECTION_ERROR when server not running")
    void postVoid_ValidToken_ConnectionRefused_ThrowsConnectionError() {
        apiClient.setAuthState("valid-token", "refresh-token", null);
        assertThatThrownBy(() -> apiClient.postVoid("/test", Map.of("data", "value")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo("CONNECTION_ERROR"));
    }

    @Test
    @DisplayName("postVoid with null body and valid token throws CONNECTION_ERROR")
    void postVoid_NullBody_ValidToken_ConnectionRefused() {
        apiClient.setAuthState("valid-token", "refresh-token", null);
        assertThatThrownBy(() -> apiClient.postVoid("/test", null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo("CONNECTION_ERROR"));
    }

    @Test
    @DisplayName("post with ParameterizedTypeReference and valid token throws CONNECTION_ERROR")
    void postWithTypeRef_ValidToken_ConnectionRefused() {
        apiClient.setAuthState("valid-token", "refresh-token", null);
        assertThatThrownBy(() -> apiClient.post("/test", Map.of("data", "value"), new ParameterizedTypeReference<Map<String, Object>>() {}))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo("CONNECTION_ERROR"));
    }

    @Test
    @DisplayName("post with ParameterizedTypeReference and null body throws CONNECTION_ERROR")
    void postWithTypeRef_NullBody_ValidToken_ConnectionRefused() {
        apiClient.setAuthState("valid-token", "refresh-token", null);
        assertThatThrownBy(() -> apiClient.post("/test", null, new ParameterizedTypeReference<Map<String, Object>>() {}))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo("CONNECTION_ERROR"));
    }

    @Test
    @DisplayName("get with ParameterizedTypeReference and valid token throws CONNECTION_ERROR")
    void getWithTypeRef_ValidToken_ConnectionRefused() {
        apiClient.setAuthState("valid-token", "refresh-token", null);
        assertThatThrownBy(() -> apiClient.get("/test", new ParameterizedTypeReference<Map<String, Object>>() {}))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo("CONNECTION_ERROR"));
    }

    // =========================================================================
    // Token refresh tests
    // =========================================================================

    @Test
    @DisplayName("get with expired token tries to refresh and fails with TOKEN_REFRESH_FAILED")
    void get_ExpiredToken_RefreshFails_ThrowsApiException() {
        // Set token with 0 expiresIn so it's already expired
        apiClient.setAuthState("expired-token", "refresh-token", 0L);
        assertThatThrownBy(() -> apiClient.get("/test", String.class))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiEx = (ApiException) e;
                    assertThat(apiEx.getErrorCode()).isEqualTo("TOKEN_REFRESH_FAILED");
                    assertThat(apiEx.getStatusCode()).isEqualTo(401);
                    assertThat(apiEx.getMessage()).contains("Session expired");
                });
        // After failed refresh, auth should be cleared
        assertThat(apiClient.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("post with expired token tries to refresh and fails")
    void post_ExpiredToken_RefreshFails_ThrowsApiException() {
        apiClient.setAuthState("expired-token", "refresh-token", 0L);
        assertThatThrownBy(() -> apiClient.post("/test", null, String.class))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo("TOKEN_REFRESH_FAILED"));
        assertThat(apiClient.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("postVoid with expired token tries to refresh and fails")
    void postVoid_ExpiredToken_RefreshFails_ThrowsApiException() {
        apiClient.setAuthState("expired-token", "refresh-token", 0L);
        assertThatThrownBy(() -> apiClient.postVoid("/test", null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo("TOKEN_REFRESH_FAILED"));
    }
}

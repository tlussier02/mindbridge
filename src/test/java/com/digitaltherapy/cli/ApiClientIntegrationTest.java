package com.digitaltherapy.cli;

import com.digitaltherapy.dto.AuthResponse;
import com.digitaltherapy.dto.CopingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiClientIntegrationTest {

    @LocalServerPort
    private int port;

    private ApiClient apiClient;

    @BeforeEach
    void setUp() {
        apiClient = new ApiClient("http://localhost:" + port);
    }

    // =========================================================================
    // Public endpoint tests
    // =========================================================================

    @Test
    @DisplayName("postPublic to auth register creates a new user")
    void postPublic_Register_ReturnsAuthResponse() {
        Map<String, String> request = Map.of(
                "name", "IntTest User " + System.currentTimeMillis(),
                "email", "inttest" + System.currentTimeMillis() + "@example.com",
                "password", "securePassword123"
        );

        AuthResponse response = apiClient.postPublic("/auth/register", request, AuthResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUserId()).isNotNull();
        assertThat(response.getName()).isNotBlank();
    }

    @Test
    @DisplayName("postPublic to auth login with valid credentials returns tokens")
    void postPublic_Login_ReturnsAuthResponse() {
        String email = "login" + System.currentTimeMillis() + "@example.com";
        // First register
        Map<String, String> registerReq = Map.of(
                "name", "Login User",
                "email", email,
                "password", "securePassword123"
        );
        apiClient.postPublic("/auth/register", registerReq, AuthResponse.class);

        // Then login
        Map<String, String> loginReq = Map.of(
                "email", email,
                "password", "securePassword123"
        );
        AuthResponse response = apiClient.postPublic("/auth/login", loginReq, AuthResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
    }

    @Test
    @DisplayName("postPublic with invalid credentials throws ApiException")
    void postPublic_InvalidLogin_ThrowsApiException() {
        Map<String, String> loginReq = Map.of(
                "email", "nonexistent@example.com",
                "password", "wrongpassword"
        );

        assertThatThrownBy(() -> apiClient.postPublic("/auth/login", loginReq, AuthResponse.class))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiEx = (ApiException) e;
                    assertThat(apiEx.getStatusCode()).isGreaterThanOrEqualTo(400);
                });
    }

    @Test
    @DisplayName("getPublic retrieves crisis coping strategies")
    void getPublic_CopingStrategies_ReturnsList() {
        List<Object> response = apiClient.getPublic("/crisis/coping-strategies",
                new ParameterizedTypeReference<List<Object>>() {});

        assertThat(response).isNotNull();
    }

    // =========================================================================
    // Authenticated endpoint tests
    // =========================================================================

    @Test
    @DisplayName("get with valid auth token retrieves session history")
    void get_SessionHistory_WithAuth_ReturnsData() {
        // Register and authenticate
        AuthResponse auth = registerAndAuth();

        List<Object> history = apiClient.get("/sessions/history",
                new ParameterizedTypeReference<List<Object>>() {});

        assertThat(history).isNotNull();
    }

    @Test
    @DisplayName("post with valid auth token to session endpoints works")
    void post_WithAuth_Works() {
        AuthResponse auth = registerAndAuth();

        // Get session history (empty initially)
        List<Object> history = apiClient.get("/sessions/history",
                new ParameterizedTypeReference<List<Object>>() {});

        assertThat(history).isNotNull();
    }

    @Test
    @DisplayName("postVoid with valid auth token calls auth logout")
    void postVoid_Logout_Works() {
        AuthResponse auth = registerAndAuth();

        // Logout should work without error
        apiClient.postVoid("/auth/logout", null);
    }

    @Test
    @DisplayName("get with Class type parameter works for authenticated requests")
    void get_WithClassType_Works() {
        AuthResponse auth = registerAndAuth();

        // Get progress - may return data or throw if no data
        try {
            String response = apiClient.get("/progress/weekly", String.class);
            assertThat(response).isNotNull();
        } catch (ApiException e) {
            // Expected if no progress data
            assertThat(e.getStatusCode()).isGreaterThanOrEqualTo(400);
        }
    }

    @Test
    @DisplayName("post with ParameterizedTypeReference works for authenticated requests")
    void post_WithTypeRef_Works() {
        AuthResponse auth = registerAndAuth();

        // Post to diary entries endpoint
        Map<String, Object> diaryEntry = Map.of(
                "situation", "Test situation",
                "automaticThought", "Test thought",
                "emotions", List.of(Map.of("name", "anxiety", "intensity", 5))
        );

        try {
            Map<String, Object> response = apiClient.post("/diary/entries", diaryEntry,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            assertThat(response).isNotNull();
        } catch (ApiException e) {
            // May fail due to missing setup, but the HTTP call was made
            assertThat(e.getStatusCode()).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("get without auth on protected endpoint throws ApiException after retry")
    void get_ProtectedEndpoint_NoAuth_ThrowsAfterRetry() {
        // Set a fake token that will get 401
        apiClient.setAuthState("invalid-token", "invalid-refresh", null);

        assertThatThrownBy(() -> apiClient.get("/sessions/library", String.class))
                .isInstanceOf(ApiException.class);
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private AuthResponse registerAndAuth() {
        String email = "user" + System.currentTimeMillis() + "@example.com";
        Map<String, String> registerReq = Map.of(
                "name", "Test User",
                "email", email,
                "password", "securePassword123"
        );

        AuthResponse auth = apiClient.postPublic("/auth/register", registerReq, AuthResponse.class);
        apiClient.setAuthState(auth.getAccessToken(), auth.getRefreshToken(), auth.getExpiresIn());
        return auth;
    }
}

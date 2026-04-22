package com.digitaltherapy.cli;

import com.digitaltherapy.dto.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "app.cli.enabled", havingValue = "true")
@Slf4j
public class ApiClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    private String accessToken;
    private String refreshToken;
    private long tokenExpiresAt;

    public ApiClient(@Value("${cli.api.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        var requestFactory = new org.springframework.http.client.JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build());
        requestFactory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void setAuthState(String accessToken, String refreshToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        if (expiresIn != null) {
            this.tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000);
        }
    }

    public void clearAuthState() {
        this.accessToken = null;
        this.refreshToken = null;
        this.tokenExpiresAt = 0;
    }

    public boolean isAuthenticated() {
        return accessToken != null;
    }

    public String getAccessToken() {
        return accessToken;
    }

    // =========================================================================
    // Authenticated requests
    // =========================================================================

    public <T> T get(String path, Class<T> responseType) {
        ensureValidToken();
        return executeWithRetry(() ->
            restClient.get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(responseType)
        );
    }

    public <T> T get(String path, ParameterizedTypeReference<T> typeRef) {
        ensureValidToken();
        return executeWithRetry(() ->
            restClient.get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(typeRef)
        );
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        ensureValidToken();
        return executeWithRetry(() -> {
            var request = restClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            if (body != null) {
                request.body(body);
            }
            return request.retrieve().body(responseType);
        });
    }

    public <T> T post(String path, Object body, ParameterizedTypeReference<T> typeRef) {
        ensureValidToken();
        return executeWithRetry(() -> {
            var request = restClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            if (body != null) {
                request.body(body);
            }
            return request.retrieve().body(typeRef);
        });
    }

    public void postVoid(String path, Object body) {
        ensureValidToken();
        executeWithRetry(() -> {
            var request = restClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            if (body != null) {
                request.body(body);
            }
            request.retrieve().toBodilessEntity();
            return null;
        });
    }

    // =========================================================================
    // Public requests (no auth)
    // =========================================================================

    public <T> T postPublic(String path, Object body, Class<T> responseType) {
        try {
            var request = restClient.post().uri(path);
            if (body != null) {
                request.body(body);
            }
            return request.retrieve().body(responseType);
        } catch (RestClientResponseException e) {
            throw handleErrorResponse(e);
        } catch (ResourceAccessException e) {
            throw new ApiException(0, "CONNECTION_ERROR",
                "Cannot connect to server at " + baseUrl + ". Is the server running?");
        }
    }

    public <T> T getPublic(String path, Class<T> responseType) {
        try {
            return restClient.get()
                .uri(path)
                .retrieve()
                .body(responseType);
        } catch (RestClientResponseException e) {
            throw handleErrorResponse(e);
        } catch (ResourceAccessException e) {
            throw new ApiException(0, "CONNECTION_ERROR",
                "Cannot connect to server at " + baseUrl + ". Is the server running?");
        }
    }

    public <T> T getPublic(String path, ParameterizedTypeReference<T> typeRef) {
        try {
            return restClient.get()
                .uri(path)
                .retrieve()
                .body(typeRef);
        } catch (RestClientResponseException e) {
            throw handleErrorResponse(e);
        } catch (ResourceAccessException e) {
            throw new ApiException(0, "CONNECTION_ERROR",
                "Cannot connect to server at " + baseUrl + ". Is the server running?");
        }
    }

    // =========================================================================
    // Token management
    // =========================================================================

    private void ensureValidToken() {
        if (accessToken == null) {
            throw new ApiException(401, "NOT_AUTHENTICATED", "Please log in first.");
        }
        // Proactive refresh if expiring within 60 seconds
        if (tokenExpiresAt > 0 && System.currentTimeMillis() > (tokenExpiresAt - 60_000)) {
            refreshAccessToken();
        }
    }

    private void refreshAccessToken() {
        try {
            AuthResponse response = restClient.post()
                .uri("/auth/refresh")
                .body(Map.of("refreshToken", refreshToken))
                .retrieve()
                .body(AuthResponse.class);

            if (response != null) {
                this.accessToken = response.getAccessToken();
                this.refreshToken = response.getRefreshToken();
                if (response.getExpiresIn() != null) {
                    this.tokenExpiresAt = System.currentTimeMillis() + (response.getExpiresIn() * 1000);
                }
                log.debug("Access token refreshed successfully");
            }
        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            clearAuthState();
            throw new ApiException(401, "TOKEN_REFRESH_FAILED",
                "Session expired. Please log in again.");
        }
    }

    private <T> T executeWithRetry(Supplier<T> requestSupplier) {
        try {
            return requestSupplier.get();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401 && refreshToken != null) {
                try {
                    refreshAccessToken();
                    return requestSupplier.get();
                } catch (RestClientResponseException retryEx) {
                    throw handleErrorResponse(retryEx);
                }
            }
            throw handleErrorResponse(e);
        } catch (ResourceAccessException e) {
            throw new ApiException(0, "CONNECTION_ERROR",
                "Cannot connect to server at " + baseUrl + ". Is the server running?");
        }
    }

    private ApiException handleErrorResponse(RestClientResponseException e) {
        try {
            String body = e.getResponseBodyAsString();
            if (body != null && !body.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorMap = objectMapper.readValue(body, Map.class);
                String message = (String) errorMap.getOrDefault("message", e.getStatusText());
                String error = (String) errorMap.getOrDefault("error", "HTTP_" + e.getStatusCode().value());
                return new ApiException(e.getStatusCode().value(), error, message);
            }
        } catch (Exception parseEx) {
            // Could not parse error body
        }
        return new ApiException(
            e.getStatusCode().value(),
            "HTTP_" + e.getStatusCode().value(),
            "Server returned " + e.getStatusCode().value() + ": " + e.getStatusText()
        );
    }
}

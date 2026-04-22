package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.ApiClient;
import com.digitaltherapy.dto.AuthResponse;
import com.digitaltherapy.dto.LoginRequest;
import com.digitaltherapy.dto.RegisterRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCommandsTest {

    @Mock
    private ApiClient apiClient;

    private PrintStream originalOut;
    private ByteArrayOutputStream outputStream;

    private AtomicReference<UUID> userIdRef;
    private AtomicReference<String> tokenRef;
    private AtomicReference<String> nameRef;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        userIdRef = new AtomicReference<>();
        tokenRef = new AtomicReference<>();
        nameRef = new AtomicReference<>();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private Scanner scannerWithInput(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    // =========================================================================
    // RegisterCommand Tests
    // =========================================================================

    @Test
    @DisplayName("RegisterCommand successful registration sets user state and prints success")
    void registerCommand_SuccessfulRegistration() {
        UUID userId = UUID.randomUUID();
        AuthResponse response = AuthResponse.builder()
                .userId(userId)
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .name("Jane Doe")
                .expiresIn(3600L)
                .build();

        when(apiClient.postPublic(eq("/auth/register"), any(RegisterRequest.class), eq(AuthResponse.class)))
                .thenReturn(response);

        Scanner scanner = scannerWithInput("Jane Doe\njane@example.com\npassword123\n");
        AuthCommands.RegisterCommand command = new AuthCommands.RegisterCommand(
                scanner,
                apiClient,
                userIdRef::set,
                tokenRef::set,
                nameRef::set
        );

        command.execute();

        assertThat(userIdRef.get()).isEqualTo(userId);
        assertThat(tokenRef.get()).isEqualTo("test-access-token");
        assertThat(nameRef.get()).isEqualTo("Jane Doe");

        String output = outputStream.toString();
        assertThat(output).contains("Registration successful!");
        assertThat(output).contains("Welcome, Jane Doe!");

        verify(apiClient).postPublic(eq("/auth/register"), any(RegisterRequest.class), eq(AuthResponse.class));
        verify(apiClient).setAuthState("test-access-token", "test-refresh-token", 3600L);
    }

    @Test
    @DisplayName("RegisterCommand with empty name shows error and does not call service")
    void registerCommand_EmptyName_ShowsError() {
        Scanner scanner = scannerWithInput("\njane@example.com\npassword123\n");
        AuthCommands.RegisterCommand command = new AuthCommands.RegisterCommand(
                scanner,
                apiClient,
                userIdRef::set,
                tokenRef::set,
                nameRef::set
        );

        command.execute();

        assertThat(userIdRef.get()).isNull();
        String output = outputStream.toString();
        assertThat(output).contains("Name cannot be empty");
    }

    @Test
    @DisplayName("RegisterCommand with empty email shows error and does not call service")
    void registerCommand_EmptyEmail_ShowsError() {
        Scanner scanner = scannerWithInput("Jane Doe\n\npassword123\n");
        AuthCommands.RegisterCommand command = new AuthCommands.RegisterCommand(
                scanner,
                apiClient,
                userIdRef::set,
                tokenRef::set,
                nameRef::set
        );

        command.execute();

        assertThat(userIdRef.get()).isNull();
        String output = outputStream.toString();
        assertThat(output).contains("Email cannot be empty");
    }

    @Test
    @DisplayName("RegisterCommand with short password shows error and does not call service")
    void registerCommand_ShortPassword_ShowsError() {
        Scanner scanner = scannerWithInput("Jane Doe\njane@example.com\nshort\n");
        AuthCommands.RegisterCommand command = new AuthCommands.RegisterCommand(
                scanner,
                apiClient,
                userIdRef::set,
                tokenRef::set,
                nameRef::set
        );

        command.execute();

        assertThat(userIdRef.get()).isNull();
        String output = outputStream.toString();
        assertThat(output).contains("Password must be at least 8 characters");
    }

    @Test
    @DisplayName("RegisterCommand when service throws exception shows error message")
    void registerCommand_ServiceException_ShowsError() {
        when(apiClient.postPublic(eq("/auth/register"), any(RegisterRequest.class), eq(AuthResponse.class)))
                .thenThrow(new RuntimeException("Email already exists"));

        Scanner scanner = scannerWithInput("Jane Doe\njane@example.com\npassword123\n");
        AuthCommands.RegisterCommand command = new AuthCommands.RegisterCommand(
                scanner,
                apiClient,
                userIdRef::set,
                tokenRef::set,
                nameRef::set
        );

        command.execute();

        assertThat(userIdRef.get()).isNull();
        String output = outputStream.toString();
        assertThat(output).contains("Registration failed");
        assertThat(output).contains("Email already exists");
    }

    @Test
    @DisplayName("RegisterCommand getName returns Register")
    void registerCommand_GetName_ReturnsRegister() {
        Scanner scanner = scannerWithInput("");
        AuthCommands.RegisterCommand command = new AuthCommands.RegisterCommand(
                scanner, apiClient, userIdRef::set, tokenRef::set, nameRef::set);

        assertThat(command.getName()).isEqualTo("Register");
    }

    @Test
    @DisplayName("RegisterCommand getDescription returns non-blank description")
    void registerCommand_GetDescription_ReturnsDescription() {
        Scanner scanner = scannerWithInput("");
        AuthCommands.RegisterCommand command = new AuthCommands.RegisterCommand(
                scanner, apiClient, userIdRef::set, tokenRef::set, nameRef::set);

        assertThat(command.getDescription()).isNotBlank();
    }

    // =========================================================================
    // LoginCommand Tests
    // =========================================================================

    @Test
    @DisplayName("LoginCommand successful login sets user state and prints success")
    void loginCommand_SuccessfulLogin() {
        UUID userId = UUID.randomUUID();
        AuthResponse response = AuthResponse.builder()
                .userId(userId)
                .accessToken("login-access-token")
                .refreshToken("login-refresh-token")
                .name("John Smith")
                .expiresIn(3600L)
                .build();

        when(apiClient.postPublic(eq("/auth/login"), any(LoginRequest.class), eq(AuthResponse.class)))
                .thenReturn(response);

        Scanner scanner = scannerWithInput("john@example.com\npassword123\n");
        AuthCommands.LoginCommand command = new AuthCommands.LoginCommand(
                scanner,
                apiClient,
                userIdRef::set,
                tokenRef::set,
                nameRef::set
        );

        command.execute();

        assertThat(userIdRef.get()).isEqualTo(userId);
        assertThat(tokenRef.get()).isEqualTo("login-access-token");
        assertThat(nameRef.get()).isEqualTo("John Smith");

        String output = outputStream.toString();
        assertThat(output).contains("Login successful!");
        assertThat(output).contains("Welcome back, John Smith!");

        verify(apiClient).postPublic(eq("/auth/login"), any(LoginRequest.class), eq(AuthResponse.class));
        verify(apiClient).setAuthState("login-access-token", "login-refresh-token", 3600L);
    }

    @Test
    @DisplayName("LoginCommand with empty email shows error and does not call service")
    void loginCommand_EmptyEmail_ShowsError() {
        Scanner scanner = scannerWithInput("\npassword123\n");
        AuthCommands.LoginCommand command = new AuthCommands.LoginCommand(
                scanner,
                apiClient,
                userIdRef::set,
                tokenRef::set,
                nameRef::set
        );

        command.execute();

        assertThat(userIdRef.get()).isNull();
        String output = outputStream.toString();
        assertThat(output).contains("Email cannot be empty");
    }

    @Test
    @DisplayName("LoginCommand with empty password shows error and does not call service")
    void loginCommand_EmptyPassword_ShowsError() {
        Scanner scanner = scannerWithInput("john@example.com\n\n");
        AuthCommands.LoginCommand command = new AuthCommands.LoginCommand(
                scanner,
                apiClient,
                userIdRef::set,
                tokenRef::set,
                nameRef::set
        );

        command.execute();

        assertThat(userIdRef.get()).isNull();
        String output = outputStream.toString();
        assertThat(output).contains("Password cannot be empty");
    }

    @Test
    @DisplayName("LoginCommand when authentication fails shows error message")
    void loginCommand_AuthenticationFailure_ShowsError() {
        when(apiClient.postPublic(eq("/auth/login"), any(LoginRequest.class), eq(AuthResponse.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        Scanner scanner = scannerWithInput("john@example.com\nwrongpassword\n");
        AuthCommands.LoginCommand command = new AuthCommands.LoginCommand(
                scanner,
                apiClient,
                userIdRef::set,
                tokenRef::set,
                nameRef::set
        );

        command.execute();

        assertThat(userIdRef.get()).isNull();
        String output = outputStream.toString();
        assertThat(output).contains("Login failed");
        assertThat(output).contains("Invalid credentials");
    }

    @Test
    @DisplayName("LoginCommand getName returns Login")
    void loginCommand_GetName_ReturnsLogin() {
        Scanner scanner = scannerWithInput("");
        AuthCommands.LoginCommand command = new AuthCommands.LoginCommand(
                scanner, apiClient, userIdRef::set, tokenRef::set, nameRef::set);

        assertThat(command.getName()).isEqualTo("Login");
    }

    @Test
    @DisplayName("LoginCommand getDescription returns non-blank description")
    void loginCommand_GetDescription_ReturnsDescription() {
        Scanner scanner = scannerWithInput("");
        AuthCommands.LoginCommand command = new AuthCommands.LoginCommand(
                scanner, apiClient, userIdRef::set, tokenRef::set, nameRef::set);

        assertThat(command.getDescription()).isNotBlank();
    }

    // =========================================================================
    // LogoutCommand Tests
    // =========================================================================

    @Test
    @DisplayName("LogoutCommand successful logout clears state and prints success")
    void logoutCommand_SuccessfulLogout() {
        tokenRef.set("my-access-token");
        userIdRef.set(UUID.randomUUID());
        nameRef.set("TestUser");

        AuthCommands.LogoutCommand command = new AuthCommands.LogoutCommand(
                apiClient,
                userIdRef::set,
                tokenRef::set,
                tokenRef::get,
                nameRef::set
        );

        command.execute();

        assertThat(userIdRef.get()).isNull();
        assertThat(tokenRef.get()).isNull();
        assertThat(nameRef.get()).isNull();

        String output = outputStream.toString();
        assertThat(output).contains("logged out successfully");

        verify(apiClient).postVoid("/auth/logout", null);
        verify(apiClient).clearAuthState();
    }

    @Test
    @DisplayName("LogoutCommand with null token skips server call but clears state")
    void logoutCommand_NullToken_SkipsServerCall() {
        tokenRef.set(null);
        userIdRef.set(UUID.randomUUID());

        AuthCommands.LogoutCommand command = new AuthCommands.LogoutCommand(
                apiClient,
                userIdRef::set,
                tokenRef::set,
                tokenRef::get,
                nameRef::set
        );

        command.execute();

        assertThat(userIdRef.get()).isNull();
        verify(apiClient).clearAuthState();
        String output = outputStream.toString();
        assertThat(output).contains("logged out successfully");
    }

    @Test
    @DisplayName("LogoutCommand server error still clears local state")
    void logoutCommand_ServerError_ClearsLocalState() {
        tokenRef.set("token");
        userIdRef.set(UUID.randomUUID());

        org.mockito.Mockito.doThrow(new RuntimeException("Server error"))
                .when(apiClient).postVoid("/auth/logout", null);

        AuthCommands.LogoutCommand command = new AuthCommands.LogoutCommand(
                apiClient,
                userIdRef::set,
                tokenRef::set,
                tokenRef::get,
                nameRef::set
        );

        command.execute();

        assertThat(userIdRef.get()).isNull();
        assertThat(tokenRef.get()).isNull();

        String output = outputStream.toString();
        assertThat(output).contains("Logged out locally");
        assertThat(output).contains("Server error");
    }

    @Test
    @DisplayName("LogoutCommand getName returns Logout")
    void logoutCommand_GetName_ReturnsLogout() {
        AuthCommands.LogoutCommand command = new AuthCommands.LogoutCommand(
                apiClient, userIdRef::set, tokenRef::set, tokenRef::get, nameRef::set);
        assertThat(command.getName()).isEqualTo("Logout");
    }

    @Test
    @DisplayName("LogoutCommand getDescription returns non-blank")
    void logoutCommand_GetDescription_ReturnsDescription() {
        AuthCommands.LogoutCommand command = new AuthCommands.LogoutCommand(
                apiClient, userIdRef::set, tokenRef::set, tokenRef::get, nameRef::set);
        assertThat(command.getDescription()).isNotBlank();
    }

    // =========================================================================
    // AuthMenuCommand Tests
    // =========================================================================

    @Test
    @DisplayName("AuthMenuCommand when logged in shows Logout option and Back")
    void authMenuCommand_LoggedIn_ShowsLogoutMenu() {
        userIdRef.set(UUID.randomUUID());
        // Input: select "2" (Back)
        Scanner scanner = scannerWithInput("2\n");

        AuthCommands.AuthMenuCommand command = new AuthCommands.AuthMenuCommand(
                scanner, apiClient,
                userIdRef::get, userIdRef::set,
                tokenRef::set, tokenRef::get,
                nameRef::set
        );

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Authentication");
        assertThat(output).contains("Logout");
        assertThat(output).contains("Back to Main Menu");
    }

    @Test
    @DisplayName("AuthMenuCommand when not logged in shows Register and Login options")
    void authMenuCommand_NotLoggedIn_ShowsRegisterAndLogin() {
        userIdRef.set(null);
        // Input: select "3" (Back)
        Scanner scanner = scannerWithInput("3\n");

        AuthCommands.AuthMenuCommand command = new AuthCommands.AuthMenuCommand(
                scanner, apiClient,
                userIdRef::get, userIdRef::set,
                tokenRef::set, tokenRef::get,
                nameRef::set
        );

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Authentication");
        assertThat(output).contains("Register");
        assertThat(output).contains("Login");
        assertThat(output).contains("Back to Main Menu");
    }

    @Test
    @DisplayName("AuthMenuCommand getName shows logged in status when user is logged in")
    void authMenuCommand_GetName_LoggedIn() {
        userIdRef.set(UUID.randomUUID());
        Scanner scanner = scannerWithInput("");

        AuthCommands.AuthMenuCommand command = new AuthCommands.AuthMenuCommand(
                scanner, apiClient,
                userIdRef::get, userIdRef::set,
                tokenRef::set, tokenRef::get,
                nameRef::set
        );

        assertThat(command.getName()).contains("Logged In");
    }

    @Test
    @DisplayName("AuthMenuCommand getName shows not logged in status when no user")
    void authMenuCommand_GetName_NotLoggedIn() {
        userIdRef.set(null);
        Scanner scanner = scannerWithInput("");

        AuthCommands.AuthMenuCommand command = new AuthCommands.AuthMenuCommand(
                scanner, apiClient,
                userIdRef::get, userIdRef::set,
                tokenRef::set, tokenRef::get,
                nameRef::set
        );

        assertThat(command.getName()).contains("Not Logged In");
    }

    @Test
    @DisplayName("AuthMenuCommand getDescription returns non-blank")
    void authMenuCommand_GetDescription() {
        Scanner scanner = scannerWithInput("");
        AuthCommands.AuthMenuCommand command = new AuthCommands.AuthMenuCommand(
                scanner, apiClient,
                userIdRef::get, userIdRef::set,
                tokenRef::set, tokenRef::get,
                nameRef::set
        );

        assertThat(command.getDescription()).isNotBlank();
    }
}

package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.ApiClient;
import com.digitaltherapy.cli.Command;
import com.digitaltherapy.cli.MenuHandler;
import com.digitaltherapy.dto.AuthResponse;
import com.digitaltherapy.dto.LoginRequest;
import com.digitaltherapy.dto.RegisterRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AuthCommands {

    // =========================================================================
    // Auth Sub-Menu Command (entry point from main menu)
    // =========================================================================
    public static class AuthMenuCommand implements Command {
        private final Scanner scanner;
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;
        private final Consumer<UUID> userIdSetter;
        private final Consumer<String> tokenSetter;
        private final Supplier<String> tokenSupplier;
        private final Consumer<String> nameSetter;

        public AuthMenuCommand(Scanner scanner,
                               ApiClient apiClient,
                               Supplier<UUID> currentUserIdSupplier,
                               Consumer<UUID> userIdSetter,
                               Consumer<String> tokenSetter,
                               Supplier<String> tokenSupplier,
                               Consumer<String> nameSetter) {
            this.scanner = scanner;
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
            this.userIdSetter = userIdSetter;
            this.tokenSetter = tokenSetter;
            this.tokenSupplier = tokenSupplier;
            this.nameSetter = nameSetter;
        }

        @Override
        public void execute() {
            List<Command> commands = new ArrayList<>();
            if (currentUserIdSupplier.get() == null) {
                commands.add(new RegisterCommand(scanner, apiClient, userIdSetter, tokenSetter, nameSetter));
                commands.add(new LoginCommand(scanner, apiClient, userIdSetter, tokenSetter, nameSetter));
            } else {
                commands.add(new LogoutCommand(apiClient, userIdSetter, tokenSetter, tokenSupplier, nameSetter));
            }
            commands.add(new BackCommand());

            MenuHandler menu = new MenuHandler("Authentication", scanner, commands);
            menu.handleInput();
        }

        @Override
        public String getName() {
            UUID userId = currentUserIdSupplier.get();
            return userId == null ? "Authentication (Not Logged In)" : "Authentication (Logged In)";
        }

        @Override
        public String getDescription() {
            return "Register, login, or logout";
        }
    }

    // =========================================================================
    // Register Command
    // =========================================================================
    public static class RegisterCommand implements Command {
        private final Scanner scanner;
        private final ApiClient apiClient;
        private final Consumer<UUID> userIdSetter;
        private final Consumer<String> tokenSetter;
        private final Consumer<String> nameSetter;

        public RegisterCommand(Scanner scanner,
                               ApiClient apiClient,
                               Consumer<UUID> userIdSetter,
                               Consumer<String> tokenSetter,
                               Consumer<String> nameSetter) {
            this.scanner = scanner;
            this.apiClient = apiClient;
            this.userIdSetter = userIdSetter;
            this.tokenSetter = tokenSetter;
            this.nameSetter = nameSetter;
        }

        @Override
        public void execute() {
            try {
                System.out.println("\n--- Register New Account ---");

                System.out.print("Enter your name: ");
                String name = scanner.nextLine().trim();
                if (name.isEmpty()) {
                    System.out.println("Name cannot be empty.");
                    return;
                }

                System.out.print("Enter your email: ");
                String email = scanner.nextLine().trim();
                if (email.isEmpty()) {
                    System.out.println("Email cannot be empty.");
                    return;
                }

                System.out.print("Enter your password (min 8 characters): ");
                String password = scanner.nextLine().trim();
                if (password.length() < 8) {
                    System.out.println("Password must be at least 8 characters.");
                    return;
                }

                RegisterRequest request = RegisterRequest.builder()
                        .name(name)
                        .email(email)
                        .password(password)
                        .build();

                AuthResponse response = apiClient.postPublic("/auth/register", request, AuthResponse.class);
                apiClient.setAuthState(response.getAccessToken(), response.getRefreshToken(), response.getExpiresIn());
                userIdSetter.accept(response.getUserId());
                tokenSetter.accept(response.getAccessToken());
                nameSetter.accept(response.getName());

                System.out.println("\n----------------------------------------");
                System.out.println("  Registration successful!");
                System.out.println("  Welcome, " + response.getName() + "!");
                System.out.println("  User ID: " + response.getUserId());
                System.out.println("----------------------------------------");
            } catch (Exception e) {
                System.out.println("Registration failed: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "Register";
        }

        @Override
        public String getDescription() {
            return "Create a new account";
        }
    }

    // =========================================================================
    // Login Command
    // =========================================================================
    public static class LoginCommand implements Command {
        private final Scanner scanner;
        private final ApiClient apiClient;
        private final Consumer<UUID> userIdSetter;
        private final Consumer<String> tokenSetter;
        private final Consumer<String> nameSetter;

        public LoginCommand(Scanner scanner,
                            ApiClient apiClient,
                            Consumer<UUID> userIdSetter,
                            Consumer<String> tokenSetter,
                            Consumer<String> nameSetter) {
            this.scanner = scanner;
            this.apiClient = apiClient;
            this.userIdSetter = userIdSetter;
            this.tokenSetter = tokenSetter;
            this.nameSetter = nameSetter;
        }

        @Override
        public void execute() {
            try {
                System.out.println("\n--- Login ---");

                System.out.print("Enter your email: ");
                String email = scanner.nextLine().trim();
                if (email.isEmpty()) {
                    System.out.println("Email cannot be empty.");
                    return;
                }

                System.out.print("Enter your password: ");
                String password = scanner.nextLine().trim();
                if (password.isEmpty()) {
                    System.out.println("Password cannot be empty.");
                    return;
                }

                LoginRequest request = LoginRequest.builder()
                        .email(email)
                        .password(password)
                        .build();

                AuthResponse response = apiClient.postPublic("/auth/login", request, AuthResponse.class);
                apiClient.setAuthState(response.getAccessToken(), response.getRefreshToken(), response.getExpiresIn());
                userIdSetter.accept(response.getUserId());
                tokenSetter.accept(response.getAccessToken());
                nameSetter.accept(response.getName());

                System.out.println("\n----------------------------------------");
                System.out.println("  Login successful!");
                System.out.println("  Welcome back, " + response.getName() + "!");
                System.out.println("----------------------------------------");
            } catch (Exception e) {
                System.out.println("Login failed: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "Login";
        }

        @Override
        public String getDescription() {
            return "Log into your account";
        }
    }

    // =========================================================================
    // Logout Command
    // =========================================================================
    public static class LogoutCommand implements Command {
        private final ApiClient apiClient;
        private final Consumer<UUID> userIdSetter;
        private final Consumer<String> tokenSetter;
        private final Supplier<String> tokenSupplier;
        private final Consumer<String> nameSetter;

        public LogoutCommand(ApiClient apiClient,
                             Consumer<UUID> userIdSetter,
                             Consumer<String> tokenSetter,
                             Supplier<String> tokenSupplier,
                             Consumer<String> nameSetter) {
            this.apiClient = apiClient;
            this.userIdSetter = userIdSetter;
            this.tokenSetter = tokenSetter;
            this.tokenSupplier = tokenSupplier;
            this.nameSetter = nameSetter;
        }

        @Override
        public void execute() {
            try {
                String token = tokenSupplier.get();
                if (token != null) {
                    apiClient.postVoid("/auth/logout", null);
                }
                apiClient.clearAuthState();
                userIdSetter.accept(null);
                tokenSetter.accept(null);
                nameSetter.accept(null);

                System.out.println("\n----------------------------------------");
                System.out.println("  You have been logged out successfully.");
                System.out.println("----------------------------------------");
            } catch (Exception e) {
                // Clear local state even if server-side logout fails
                apiClient.clearAuthState();
                userIdSetter.accept(null);
                tokenSetter.accept(null);
                nameSetter.accept(null);
                System.out.println("Logged out locally. Server error: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "Logout";
        }

        @Override
        public String getDescription() {
            return "Log out of your account";
        }
    }

    // =========================================================================
    // Back Command (return to main menu)
    // =========================================================================
    private static class BackCommand implements Command {
        @Override
        public void execute() {
            // Do nothing - returns to main menu
        }

        @Override
        public String getName() {
            return "Back to Main Menu";
        }

        @Override
        public String getDescription() {
            return "Return to the main menu";
        }
    }
}

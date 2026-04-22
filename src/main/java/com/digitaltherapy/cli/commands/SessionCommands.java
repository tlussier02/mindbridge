package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.ApiClient;
import com.digitaltherapy.cli.Command;
import com.digitaltherapy.cli.MenuHandler;
import com.digitaltherapy.dto.*;
import org.springframework.core.ParameterizedTypeReference;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Supplier;

public class SessionCommands {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // =========================================================================
    // Session Sub-Menu Command (entry point from main menu)
    // =========================================================================
    public static class SessionMenuCommand implements Command {
        private final Scanner scanner;
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public SessionMenuCommand(Scanner scanner,
                                  ApiClient apiClient,
                                  Supplier<UUID> currentUserIdSupplier) {
            this.scanner = scanner;
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            if (currentUserIdSupplier.get() == null) {
                System.out.println("\nPlease log in first to access CBT Sessions.");
                return;
            }

            List<Command> commands = new ArrayList<>();
            commands.add(new ViewSessionLibraryCommand(scanner, apiClient, currentUserIdSupplier));
            commands.add(new StartNewSessionCommand(scanner, apiClient, currentUserIdSupplier));
            commands.add(new ViewSessionHistoryCommand(scanner, apiClient, currentUserIdSupplier));
            commands.add(new BackCommand());

            MenuHandler menu = new MenuHandler("CBT Sessions", scanner, commands);
            menu.handleInput();
        }

        @Override
        public String getName() {
            return "CBT Sessions";
        }

        @Override
        public String getDescription() {
            return "Browse and start CBT therapy sessions";
        }
    }

    // =========================================================================
    // View Session Library Command
    // =========================================================================
    public static class ViewSessionLibraryCommand implements Command {
        private final Scanner scanner;
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public ViewSessionLibraryCommand(Scanner scanner,
                                         ApiClient apiClient,
                                         Supplier<UUID> currentUserIdSupplier) {
            this.scanner = scanner;
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            try {
                List<SessionModuleDto> modules = apiClient.get("/sessions",
                    new ParameterizedTypeReference<List<SessionModuleDto>>() {});

                if (modules == null || modules.isEmpty()) {
                    System.out.println("\nNo session modules available at this time.");
                    return;
                }

                System.out.println("\n========================================");
                System.out.println("  Session Library");
                System.out.println("========================================");

                for (SessionModuleDto module : modules) {
                    System.out.println("\n  Module: " + module.getName());
                    System.out.println("  Category: " + module.getCategory());
                    System.out.println("  " + module.getDescription());

                    if (module.getSessions() != null && !module.getSessions().isEmpty()) {
                        System.out.println("  Sessions:");
                        for (SessionModuleDto.SessionSummaryItem session : module.getSessions()) {
                            System.out.printf("    - [%s] %s (%d min)%n",
                                    session.getId().toString().substring(0, 8),
                                    session.getTitle(),
                                    session.getDurationMinutes());
                        }
                    }
                    System.out.println("  ----------------------------------------");
                }
            } catch (Exception e) {
                System.out.println("Failed to load session library: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "View Session Library";
        }

        @Override
        public String getDescription() {
            return "Browse available CBT session modules";
        }
    }

    // =========================================================================
    // Start New Session Command
    // =========================================================================
    public static class StartNewSessionCommand implements Command {
        private final Scanner scanner;
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public StartNewSessionCommand(Scanner scanner,
                                      ApiClient apiClient,
                                      Supplier<UUID> currentUserIdSupplier) {
            this.scanner = scanner;
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            try {
                // First show the library so user can pick a session
                List<SessionModuleDto> modules = apiClient.get("/sessions",
                    new ParameterizedTypeReference<List<SessionModuleDto>>() {});

                if (modules == null || modules.isEmpty()) {
                    System.out.println("\nNo session modules available at this time.");
                    return;
                }

                // Collect all available sessions for selection
                List<SessionModuleDto.SessionSummaryItem> allSessions = new ArrayList<>();
                System.out.println("\n========================================");
                System.out.println("  Available Sessions");
                System.out.println("========================================");

                int index = 1;
                for (SessionModuleDto module : modules) {
                    if (module.getSessions() != null) {
                        for (SessionModuleDto.SessionSummaryItem session : module.getSessions()) {
                            System.out.printf("  %d. [%s] %s (%d min)%n",
                                    index, module.getName(), session.getTitle(), session.getDurationMinutes());
                            allSessions.add(session);
                            index++;
                        }
                    }
                }
                System.out.println("  0. Cancel");
                System.out.println("========================================");

                if (allSessions.isEmpty()) {
                    System.out.println("No sessions available to start.");
                    return;
                }

                System.out.print("Select a session to start: ");
                String input = scanner.nextLine().trim();
                int choice;
                try {
                    choice = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid number.");
                    return;
                }

                if (choice == 0) {
                    return;
                }
                if (choice < 1 || choice > allSessions.size()) {
                    System.out.println("Invalid selection.");
                    return;
                }

                SessionModuleDto.SessionSummaryItem selected = allSessions.get(choice - 1);
                ActiveSession active = apiClient.post(
                    "/sessions/" + selected.getId() + "/start",
                    Map.of(), ActiveSession.class);

                System.out.println("\n========================================");
                System.out.println("  Session Started: " + active.getTitle());
                System.out.println("  " + active.getDescription());
                System.out.println("========================================");
                System.out.println("  Type your messages to interact with the therapist.");
                System.out.println("  Type 'exit' or 'quit' to end the session.");
                System.out.println("========================================\n");

                // Enter chat loop
                UUID sessionId = active.getUserSessionId();
                while (true) {
                    System.out.print("You: ");
                    String message = scanner.nextLine().trim();

                    if (message.isEmpty()) {
                        continue;
                    }

                    if (message.equalsIgnoreCase("exit") || message.equalsIgnoreCase("quit")) {
                        try {
                            var summary = apiClient.post(
                                "/sessions/" + sessionId + "/end",
                                Map.of("reason", "User ended session"),
                                SessionSummary.class);
                            System.out.println("\n========================================");
                            System.out.println("  Session Ended");
                            System.out.println("========================================");
                            if (summary.getSummary() != null) {
                                System.out.println("  Summary: " + summary.getSummary());
                            }
                            if (summary.getMoodBefore() != null && summary.getMoodAfter() != null) {
                                System.out.println("  Mood: " + summary.getMoodBefore() + " -> " + summary.getMoodAfter());
                            }
                            if (summary.getKeyInsights() != null && !summary.getKeyInsights().isEmpty()) {
                                System.out.println("  Key Insights:");
                                for (String insight : summary.getKeyInsights()) {
                                    System.out.println("    - " + insight);
                                }
                            }
                            System.out.println("========================================");
                        } catch (Exception e) {
                            System.out.println("Session ended. Could not retrieve summary: " + e.getMessage());
                        }
                        break;
                    }

                    try {
                        ChatResponse response = apiClient.post(
                            "/sessions/" + sessionId + "/chat",
                            new ChatRequest(message, "TEXT"),
                            ChatResponse.class);
                        System.out.println("\nTherapist: " + response.getMessage());

                        if (response.isCrisisDetected()) {
                            System.out.println("\n  [ALERT] Crisis indicators detected.");
                            if (response.getCrisisAction() != null) {
                                System.out.println("  Action: " + response.getCrisisAction());
                            }
                        }
                        System.out.println();
                    } catch (Exception e) {
                        System.out.println("Error sending message: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("Failed to start session: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "Start New Session";
        }

        @Override
        public String getDescription() {
            return "Start a new CBT therapy session";
        }
    }

    // =========================================================================
    // View Session History Command
    // =========================================================================
    public static class ViewSessionHistoryCommand implements Command {
        private final Scanner scanner;
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public ViewSessionHistoryCommand(Scanner scanner,
                                         ApiClient apiClient,
                                         Supplier<UUID> currentUserIdSupplier) {
            this.scanner = scanner;
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            try {
                List<SessionHistoryEntry> history = apiClient.get("/sessions/history",
                    new ParameterizedTypeReference<List<SessionHistoryEntry>>() {});

                if (history == null || history.isEmpty()) {
                    System.out.println("\nNo session history found. Start your first session!");
                    return;
                }

                System.out.println("\n========================================");
                System.out.println("  Session History");
                System.out.println("========================================");

                for (SessionHistoryEntry entry : history) {
                    System.out.printf("  Session: %s%n", entry.getSessionTitle());
                    System.out.printf("  Module:  %s%n", entry.getModuleName());
                    System.out.printf("  Status:  %s%n", entry.getStatus());
                    if (entry.getStartedAt() != null) {
                        System.out.printf("  Started: %s%n", entry.getStartedAt().format(DATE_FMT));
                    }
                    if (entry.getEndedAt() != null) {
                        System.out.printf("  Ended:   %s%n", entry.getEndedAt().format(DATE_FMT));
                    }
                    if (entry.getMoodBefore() != null && entry.getMoodAfter() != null) {
                        System.out.printf("  Mood:    %d -> %d%n", entry.getMoodBefore(), entry.getMoodAfter());
                    }
                    System.out.println("  ----------------------------------------");
                }
            } catch (Exception e) {
                System.out.println("Failed to load session history: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "View Session History";
        }

        @Override
        public String getDescription() {
            return "View your past therapy sessions";
        }
    }

    // =========================================================================
    // Back Command
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

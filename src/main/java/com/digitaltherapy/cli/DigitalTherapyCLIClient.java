package com.digitaltherapy.cli;

import com.digitaltherapy.cli.commands.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.cli.enabled", havingValue = "true")
public class DigitalTherapyCLIClient implements CommandLineRunner {

    private final ApiClient apiClient;

    private Scanner scanner;
    private UUID currentUserId;
    private String currentAccessToken;
    private String currentUserName;
    private boolean running = true;

    @Override
    public void run(String... args) {
        scanner = new Scanner(System.in);
        log.info("Starting Digital Therapy CLI Client");

        printWelcomeBanner();

        while (running) {
            try {
                MenuHandler mainMenu = new MenuHandler("Main Menu", scanner, buildMainMenuCommands());
                mainMenu.handleInput();
            } catch (Exception e) {
                log.error("Unexpected error in CLI loop", e);
                System.out.println("An unexpected error occurred. Please try again.");
            }
        }

        scanner.close();
        log.info("Digital Therapy CLI Client shut down");
    }

    private List<Command> buildMainMenuCommands() {
        List<Command> commands = new ArrayList<>();

        // 1. Authentication
        commands.add(new AuthCommands.AuthMenuCommand(
                scanner,
                apiClient,
                this::getCurrentUserId,
                this::setCurrentUserId,
                this::setCurrentAccessToken,
                this::getCurrentAccessToken,
                this::setCurrentUserName
        ));

        // 2. CBT Sessions
        commands.add(new SessionCommands.SessionMenuCommand(
                scanner,
                apiClient,
                this::getCurrentUserId
        ));

        // 3. Thought Diary
        commands.add(new DiaryCommands.DiaryMenuCommand(
                scanner,
                apiClient,
                this::getCurrentUserId
        ));

        // 4. Progress Dashboard
        commands.add(new ProgressCommands.ProgressMenuCommand(
                scanner,
                apiClient,
                this::getCurrentUserId
        ));

        // 5. Crisis Support
        commands.add(new CrisisCommands.CrisisMenuCommand(
                scanner,
                apiClient,
                this::getCurrentUserId
        ));

        // 6. Settings
        commands.add(new SettingsCommand(
                this::getCurrentUserId,
                this::getCurrentUserName,
                apiClient
        ));

        // 7. Exit
        commands.add(new ExitCommand(() -> running = false));

        return commands;
    }

    private void printWelcomeBanner() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("                                        ");
        System.out.println("   Digital Therapy Assistant             ");
        System.out.println("   Your CBT Companion                   ");
        System.out.println("                                        ");
        System.out.println("   Cognitive Behavioral Therapy tools    ");
        System.out.println("   to support your mental wellness.     ");
        System.out.println("                                        ");
        System.out.println("========================================");
        System.out.println();
    }

    // =========================================================================
    // State accessor/mutator methods used as Supplier/Consumer references
    // =========================================================================

    private UUID getCurrentUserId() {
        return currentUserId;
    }

    private void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
    }

    private String getCurrentAccessToken() {
        return currentAccessToken;
    }

    private void setCurrentAccessToken(String token) {
        this.currentAccessToken = token;
    }

    private String getCurrentUserName() {
        return currentUserName;
    }

    private void setCurrentUserName(String name) {
        this.currentUserName = name;
    }
}

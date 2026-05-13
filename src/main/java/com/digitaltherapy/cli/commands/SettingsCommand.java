package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.ApiClient;
import com.digitaltherapy.cli.Command;
import com.digitaltherapy.dto.WeeklyProgress;

import java.util.UUID;
import java.util.function.Supplier;

public class SettingsCommand implements Command {
    private final Supplier<UUID> currentUserIdSupplier;
    private final Supplier<String> currentUserNameSupplier;
    private final ApiClient apiClient;

    public SettingsCommand(Supplier<UUID> currentUserIdSupplier,
                           Supplier<String> currentUserNameSupplier,
                           ApiClient apiClient) {
        this.currentUserIdSupplier = currentUserIdSupplier;
        this.currentUserNameSupplier = currentUserNameSupplier;
        this.apiClient = apiClient;
    }

    @Override
    public void execute() {
        UUID userId = currentUserIdSupplier.get();

        System.out.println("\n========================================");
        System.out.println("  Settings & Profile");
        System.out.println("========================================");

        if (userId == null) {
            System.out.println("  Status:  Not logged in");
            System.out.println("  Please log in to view your profile.");
        } else {
            String name = currentUserNameSupplier.get();
            System.out.printf("  Name:    %s%n", name != null ? name : "N/A");
            System.out.printf("  User ID: %s%n", userId);
            System.out.println("  Status:  Logged in");

            // Try to fetch streak days from weekly progress
            try {
                WeeklyProgress progress = apiClient.get("/progress/weekly", WeeklyProgress.class);
                if (progress != null) {
                    System.out.printf("  Streak:  %d days%n", progress.getStreakDays());
                }
            } catch (Exception e) {
                System.out.println("  Streak:  Unable to load");
            }
        }

        System.out.println("\n  Application Info:");
        System.out.println("  ----------------------------------------");
        System.out.println("  App:     Digital Therapy Assistant");
        System.out.println("  Version: 1.0.0");
        System.out.println("  Runtime: Java " + System.getProperty("java.version"));
        System.out.println("========================================");
    }

    @Override
    public String getName() {
        return "Settings";
    }

    @Override
    public String getDescription() {
        return "View profile and application settings";
    }
}

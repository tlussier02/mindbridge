package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.ApiClient;
import com.digitaltherapy.cli.Command;
import com.digitaltherapy.cli.MenuHandler;
import com.digitaltherapy.dto.Achievement;
import com.digitaltherapy.dto.MonthlyTrend;
import com.digitaltherapy.dto.WeeklyProgress;
import org.springframework.core.ParameterizedTypeReference;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Supplier;

public class ProgressCommands {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // =========================================================================
    // Progress Sub-Menu Command (entry point from main menu)
    // =========================================================================
    public static class ProgressMenuCommand implements Command {
        private final Scanner scanner;
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public ProgressMenuCommand(Scanner scanner,
                                   ApiClient apiClient,
                                   Supplier<UUID> currentUserIdSupplier) {
            this.scanner = scanner;
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            if (currentUserIdSupplier.get() == null) {
                System.out.println("\nPlease log in first to access the Progress Dashboard.");
                return;
            }

            List<Command> commands = new ArrayList<>();
            commands.add(new WeeklySummaryCommand(apiClient, currentUserIdSupplier));
            commands.add(new MonthlyTrendsCommand(apiClient, currentUserIdSupplier));
            commands.add(new AchievementsCommand(apiClient, currentUserIdSupplier));
            commands.add(new BackCommand());

            MenuHandler menu = new MenuHandler("Progress Dashboard", scanner, commands);
            menu.handleInput();
        }

        @Override
        public String getName() {
            return "Progress Dashboard";
        }

        @Override
        public String getDescription() {
            return "View your therapy progress and achievements";
        }
    }

    // =========================================================================
    // Weekly Summary Command
    // =========================================================================
    public static class WeeklySummaryCommand implements Command {
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public WeeklySummaryCommand(ApiClient apiClient,
                                    Supplier<UUID> currentUserIdSupplier) {
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            try {
                WeeklyProgress progress = apiClient.get("/progress/weekly", WeeklyProgress.class);

                if (progress == null) {
                    System.out.println("\nNo weekly progress data available yet.");
                    return;
                }

                System.out.println("\n========================================");
                System.out.println("  Weekly Progress Summary");
                System.out.println("========================================");
                if (progress.getWeekStart() != null && progress.getWeekEnd() != null) {
                    System.out.printf("  Week: %s to %s%n",
                            progress.getWeekStart().format(DATE_FMT),
                            progress.getWeekEnd().format(DATE_FMT));
                }
                System.out.printf("  Sessions Completed:  %d%n", progress.getSessionsCompleted());
                System.out.printf("  Diary Entries:       %d%n", progress.getDiaryEntries());
                System.out.printf("  Average Mood:        %.1f%n", progress.getAverageMood());
                System.out.printf("  Streak Days:         %d%n", progress.getStreakDays());

                if (progress.getDailyMoods() != null && !progress.getDailyMoods().isEmpty()) {
                    System.out.println("\n  Daily Mood Breakdown:");
                    System.out.println("  ----------------------------------------");
                    System.out.printf("  %-12s  %-10s  %-8s%n", "Date", "Avg Mood", "Entries");
                    System.out.println("  ----------------------------------------");
                    for (WeeklyProgress.DailyMood daily : progress.getDailyMoods()) {
                        System.out.printf("  %-12s  %-10.1f  %-8d%n",
                                daily.getDate() != null ? daily.getDate().format(DATE_FMT) : "N/A",
                                daily.getAverageMood(),
                                daily.getEntriesCount());
                    }
                }
                System.out.println("========================================");
            } catch (Exception e) {
                System.out.println("Failed to load weekly progress: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "Weekly Summary";
        }

        @Override
        public String getDescription() {
            return "View this week's progress summary";
        }
    }

    // =========================================================================
    // Monthly Trends Command
    // =========================================================================
    public static class MonthlyTrendsCommand implements Command {
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public MonthlyTrendsCommand(ApiClient apiClient,
                                    Supplier<UUID> currentUserIdSupplier) {
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            try {
                MonthlyTrend trend = apiClient.get("/progress/monthly", MonthlyTrend.class);

                if (trend == null) {
                    System.out.println("\nNo monthly trend data available yet.");
                    return;
                }

                System.out.println("\n========================================");
                System.out.println("  Monthly Trends");
                System.out.println("========================================");
                System.out.printf("  Month:               %s %d%n", trend.getMonth(), trend.getYear());
                System.out.printf("  Total Sessions:      %d%n", trend.getTotalSessions());
                System.out.printf("  Total Diary Entries: %d%n", trend.getTotalDiaryEntries());
                System.out.printf("  Avg Mood (Start):    %.1f%n", trend.getAverageMoodStart());
                System.out.printf("  Avg Mood (End):      %.1f%n", trend.getAverageMoodEnd());
                System.out.printf("  Mood Trend:          %s%.1f%n",
                        trend.getMoodTrend() >= 0 ? "+" : "", trend.getMoodTrend());

                if (trend.getWeeks() != null && !trend.getWeeks().isEmpty()) {
                    System.out.println("\n  Weekly Breakdown:");
                    System.out.println("  ----------------------------------------");
                    System.out.printf("  %-8s  %-10s  %-8s  %-10s%n", "Week", "Sessions", "Entries", "Avg Mood");
                    System.out.println("  ----------------------------------------");
                    for (MonthlyTrend.WeeklySummaryItem week : trend.getWeeks()) {
                        System.out.printf("  %-8d  %-10d  %-8d  %-10.1f%n",
                                week.getWeekNumber(),
                                week.getSessions(),
                                week.getEntries(),
                                week.getAvgMood());
                    }
                }
                System.out.println("========================================");
            } catch (Exception e) {
                System.out.println("Failed to load monthly trends: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "Monthly Trends";
        }

        @Override
        public String getDescription() {
            return "View monthly trend analysis";
        }
    }

    // =========================================================================
    // Achievements Command
    // =========================================================================
    public static class AchievementsCommand implements Command {
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public AchievementsCommand(ApiClient apiClient,
                                   Supplier<UUID> currentUserIdSupplier) {
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            try {
                List<Achievement> achievements = apiClient.get("/progress/achievements",
                    new ParameterizedTypeReference<List<Achievement>>() {});

                if (achievements == null || achievements.isEmpty()) {
                    System.out.println("\nNo achievements yet. Keep going!");
                    return;
                }

                System.out.println("\n========================================");
                System.out.println("  Achievements");
                System.out.println("========================================");

                int unlocked = 0;
                int total = achievements.size();

                for (Achievement achievement : achievements) {
                    String status = achievement.isUnlocked() ? "[UNLOCKED]" : String.format("[%.0f%%]", achievement.getProgress() * 100);
                    System.out.printf("  %s %s %s%n", achievement.getIcon() != null ? achievement.getIcon() : "*", achievement.getName(), status);
                    System.out.printf("    %s%n", achievement.getDescription());
                    if (achievement.isUnlocked() && achievement.getUnlockedAt() != null) {
                        System.out.printf("    Unlocked: %s%n",
                                achievement.getUnlockedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                        unlocked++;
                    }
                    System.out.println("  ----------------------------------------");
                }

                System.out.printf("  Total: %d/%d achievements unlocked%n", unlocked, total);
                System.out.println("========================================");
            } catch (Exception e) {
                System.out.println("Failed to load achievements: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "Achievements";
        }

        @Override
        public String getDescription() {
            return "View your achievements and badges";
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

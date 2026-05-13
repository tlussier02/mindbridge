package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.ApiClient;
import com.digitaltherapy.cli.Command;
import com.digitaltherapy.cli.MenuHandler;
import com.digitaltherapy.cli.RestPage;
import com.digitaltherapy.dto.DiaryEntryCreate;
import com.digitaltherapy.dto.DiaryEntryResponse;
import com.digitaltherapy.dto.DiaryEntrySummary;
import com.digitaltherapy.dto.DiaryInsights;
import com.digitaltherapy.dto.DistortionSuggestion;
import org.springframework.core.ParameterizedTypeReference;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Supplier;

public class DiaryCommands {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // =========================================================================
    // Diary Sub-Menu Command (entry point from main menu)
    // =========================================================================
    public static class DiaryMenuCommand implements Command {
        private final Scanner scanner;
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public DiaryMenuCommand(Scanner scanner,
                                ApiClient apiClient,
                                Supplier<UUID> currentUserIdSupplier) {
            this.scanner = scanner;
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            if (currentUserIdSupplier.get() == null) {
                System.out.println("\nPlease log in first to access the Thought Diary.");
                return;
            }

            List<Command> commands = new ArrayList<>();
            commands.add(new NewEntryCommand(scanner, apiClient, currentUserIdSupplier));
            commands.add(new ViewEntriesCommand(scanner, apiClient, currentUserIdSupplier));
            commands.add(new ViewInsightsCommand(apiClient, currentUserIdSupplier));
            commands.add(new BackCommand());

            MenuHandler menu = new MenuHandler("Thought Diary", scanner, commands);
            menu.handleInput();
        }

        @Override
        public String getName() {
            return "Thought Diary";
        }

        @Override
        public String getDescription() {
            return "Record and review thought diary entries";
        }
    }

    // =========================================================================
    // New Entry Command
    // =========================================================================
    public static class NewEntryCommand implements Command {
        private final Scanner scanner;
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public NewEntryCommand(Scanner scanner,
                               ApiClient apiClient,
                               Supplier<UUID> currentUserIdSupplier) {
            this.scanner = scanner;
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            try {
                System.out.println("\n========================================");
                System.out.println("  New Thought Diary Entry");
                System.out.println("========================================");

                // Situation
                System.out.print("Describe the situation: ");
                String situation = scanner.nextLine().trim();
                if (situation.isEmpty()) {
                    System.out.println("Situation cannot be empty.");
                    return;
                }

                // Automatic thought
                System.out.print("What was your automatic thought? ");
                String automaticThought = scanner.nextLine().trim();
                if (automaticThought.isEmpty()) {
                    System.out.println("Automatic thought cannot be empty.");
                    return;
                }

                // Emotions
                List<DiaryEntryCreate.EmotionRatingDto> emotions = new ArrayList<>();
                System.out.println("\nEnter your emotions (type 'done' when finished):");
                while (true) {
                    System.out.print("  Emotion name (or 'done'): ");
                    String emotionName = scanner.nextLine().trim();
                    if (emotionName.equalsIgnoreCase("done") || emotionName.isEmpty()) {
                        break;
                    }

                    System.out.print("  Intensity (1-10): ");
                    String intensityStr = scanner.nextLine().trim();
                    int intensity;
                    try {
                        intensity = Integer.parseInt(intensityStr);
                        if (intensity < 1 || intensity > 10) {
                            System.out.println("  Intensity must be between 1 and 10. Skipping this emotion.");
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("  Invalid number. Skipping this emotion.");
                        continue;
                    }

                    emotions.add(DiaryEntryCreate.EmotionRatingDto.builder()
                            .emotion(emotionName)
                            .intensity(intensity)
                            .build());
                }

                // Mood before
                System.out.print("Mood before (1-10): ");
                Integer moodBefore = parseOptionalInt(scanner.nextLine().trim(), 1, 10);

                // Mood after
                System.out.print("Mood after (1-10): ");
                Integer moodAfter = parseOptionalInt(scanner.nextLine().trim(), 1, 10);

                // Alternative thought (optional)
                System.out.print("Alternative thought (or press Enter to skip): ");
                String alternativeThought = scanner.nextLine().trim();

                // Build the entry
                DiaryEntryCreate entry = DiaryEntryCreate.builder()
                        .situation(situation)
                        .automaticThought(automaticThought)
                        .emotions(emotions.isEmpty() ? null : emotions)
                        .moodBefore(moodBefore)
                        .moodAfter(moodAfter)
                        .alternativeThought(alternativeThought.isEmpty() ? null : alternativeThought)
                        .build();

                DiaryEntryResponse response = apiClient.post("/diary/entries", entry, DiaryEntryResponse.class);

                System.out.println("\n----------------------------------------");
                System.out.println("  Diary entry created successfully!");
                System.out.println("  Entry ID: " + response.getId());
                System.out.println("----------------------------------------");

                // Suggest distortions based on the automatic thought
                try {
                    List<DistortionSuggestion> suggestions = apiClient.post(
                        "/diary/distortions/suggest",
                        Map.of("thought", automaticThought),
                        new ParameterizedTypeReference<List<DistortionSuggestion>>() {});
                    if (suggestions != null && !suggestions.isEmpty()) {
                        System.out.println("\n  Suggested Cognitive Distortions:");
                        System.out.println("  ----------------------------------------");
                        for (DistortionSuggestion suggestion : suggestions) {
                            System.out.printf("  - %s (confidence: %.0f%%)%n",
                                    suggestion.getName(), suggestion.getConfidence() * 100);
                            System.out.printf("    Reasoning: %s%n", suggestion.getReasoning());
                        }
                        System.out.println("  ----------------------------------------");
                    }
                } catch (Exception e) {
                    System.out.println("  Could not generate distortion suggestions: " + e.getMessage());
                }
            } catch (Exception e) {
                System.out.println("Failed to create diary entry: " + e.getMessage());
            }
        }

        private Integer parseOptionalInt(String input, int min, int max) {
            if (input.isEmpty()) {
                return null;
            }
            try {
                int value = Integer.parseInt(input);
                if (value < min || value > max) {
                    System.out.printf("  Value must be between %d and %d. Skipping.%n", min, max);
                    return null;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("  Invalid number. Skipping.");
                return null;
            }
        }

        @Override
        public String getName() {
            return "New Diary Entry";
        }

        @Override
        public String getDescription() {
            return "Create a new thought diary entry";
        }
    }

    // =========================================================================
    // View Entries Command
    // =========================================================================
    public static class ViewEntriesCommand implements Command {
        private final Scanner scanner;
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        private static final int PAGE_SIZE = 5;

        public ViewEntriesCommand(Scanner scanner,
                                  ApiClient apiClient,
                                  Supplier<UUID> currentUserIdSupplier) {
            this.scanner = scanner;
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            try {
                int currentPage = 0;

                while (true) {
                    String path = "/diary/entries?page=" + currentPage + "&size=" + PAGE_SIZE + "&sort=createdAt,desc";
                    RestPage<DiaryEntrySummary> page = apiClient.get(path,
                        new ParameterizedTypeReference<RestPage<DiaryEntrySummary>>() {});

                    if (page.isEmpty() && currentPage == 0) {
                        System.out.println("\nNo diary entries found. Create your first entry!");
                        return;
                    }

                    System.out.println("\n========================================");
                    System.out.printf("  Diary Entries (Page %d of %d)%n", currentPage + 1, page.getTotalPages());
                    System.out.println("========================================");

                    for (DiaryEntrySummary entry : page.getContent()) {
                        System.out.printf("  [%s] %s%n",
                                entry.getCreatedAt() != null ? entry.getCreatedAt().format(DATE_FMT) : "N/A",
                                truncate(entry.getSituation(), 50));
                        System.out.printf("  Thought: %s%n", truncate(entry.getAutomaticThought(), 50));
                        if (entry.getMoodBefore() != null && entry.getMoodAfter() != null) {
                            System.out.printf("  Mood: %d -> %d | Distortions: %d%n",
                                    entry.getMoodBefore(), entry.getMoodAfter(), entry.getDistortionCount());
                        }
                        System.out.println("  ----------------------------------------");
                    }

                    System.out.printf("  Showing %d of %d total entries%n",
                            page.getNumberOfElements(), page.getTotalElements());

                    // Pagination controls
                    StringBuilder nav = new StringBuilder("  ");
                    if (page.hasPrevious()) {
                        nav.append("[P]revious  ");
                    }
                    if (page.hasNext()) {
                        nav.append("[N]ext  ");
                    }
                    nav.append("[B]ack");
                    System.out.println(nav);
                    System.out.print("  Choice: ");

                    String input = scanner.nextLine().trim().toUpperCase();
                    switch (input) {
                        case "P" -> {
                            if (page.hasPrevious()) {
                                currentPage--;
                            }
                        }
                        case "N" -> {
                            if (page.hasNext()) {
                                currentPage++;
                            }
                        }
                        case "B" -> {
                            return;
                        }
                        default -> System.out.println("  Invalid option.");
                    }
                }
            } catch (Exception e) {
                System.out.println("Failed to load diary entries: " + e.getMessage());
            }
        }

        private String truncate(String text, int maxLength) {
            if (text == null) {
                return "N/A";
            }
            return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
        }

        @Override
        public String getName() {
            return "View Entries";
        }

        @Override
        public String getDescription() {
            return "View your diary entries with pagination";
        }
    }

    // =========================================================================
    // View Insights Command
    // =========================================================================
    public static class ViewInsightsCommand implements Command {
        private final ApiClient apiClient;
        private final Supplier<UUID> currentUserIdSupplier;

        public ViewInsightsCommand(ApiClient apiClient,
                                   Supplier<UUID> currentUserIdSupplier) {
            this.apiClient = apiClient;
            this.currentUserIdSupplier = currentUserIdSupplier;
        }

        @Override
        public void execute() {
            try {
                DiaryInsights insights = apiClient.get("/diary/insights", DiaryInsights.class);

                if (insights == null) {
                    System.out.println("\nNo insights available yet. Keep journaling!");
                    return;
                }

                System.out.println("\n========================================");
                System.out.println("  Diary Insights");
                System.out.println("========================================");
                System.out.printf("  Total Entries:            %d%n", insights.getTotalEntries());
                System.out.printf("  Avg Mood Improvement:     %.1f%n", insights.getAverageMoodImprovement());

                if (insights.getTopDistortions() != null && !insights.getTopDistortions().isEmpty()) {
                    System.out.println("\n  Top Cognitive Distortions:");
                    for (DiaryInsights.DistortionFrequency df : insights.getTopDistortions()) {
                        System.out.printf("    - %s: %d occurrences%n", df.getName(), df.getCount());
                    }
                }

                if (insights.getPatterns() != null && !insights.getPatterns().isEmpty()) {
                    System.out.println("\n  Patterns Identified:");
                    for (String pattern : insights.getPatterns()) {
                        System.out.println("    - " + pattern);
                    }
                }

                if (insights.getRecommendations() != null && !insights.getRecommendations().isEmpty()) {
                    System.out.println("\n  Recommendations:");
                    for (String rec : insights.getRecommendations()) {
                        System.out.println("    - " + rec);
                    }
                }
                System.out.println("========================================");
            } catch (Exception e) {
                System.out.println("Failed to load insights: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "View Insights";
        }

        @Override
        public String getDescription() {
            return "View insights from your diary entries";
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

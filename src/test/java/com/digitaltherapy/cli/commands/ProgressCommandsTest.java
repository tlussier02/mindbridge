package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.ApiClient;
import com.digitaltherapy.dto.Achievement;
import com.digitaltherapy.dto.MonthlyTrend;
import com.digitaltherapy.dto.WeeklyProgress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressCommandsTest {

    @Mock
    private ApiClient apiClient;

    private PrintStream originalOut;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String capturedOutput() {
        return outputStream.toString();
    }

    // =========================================================================
    // ProgressMenuCommand Tests
    // =========================================================================

    @Test
    @DisplayName("ProgressMenuCommand when user is not logged in prints login prompt")
    void progressMenuCommand_NotLoggedIn_PrintsLoginPrompt() {
        Supplier<UUID> nullUserSupplier = () -> null;
        Scanner scanner = new Scanner(System.in);

        ProgressCommands.ProgressMenuCommand command =
                new ProgressCommands.ProgressMenuCommand(scanner, apiClient, nullUserSupplier);

        command.execute();

        assertThat(capturedOutput()).contains("Please log in first to access the Progress Dashboard.");
    }

    @Test
    @DisplayName("ProgressMenuCommand getName returns Progress Dashboard")
    void progressMenuCommand_GetName_ReturnsProgressDashboard() {
        Supplier<UUID> userSupplier = () -> null;
        Scanner scanner = new Scanner(System.in);

        ProgressCommands.ProgressMenuCommand command =
                new ProgressCommands.ProgressMenuCommand(scanner, apiClient, userSupplier);

        assertThat(command.getName()).isEqualTo("Progress Dashboard");
    }

    // =========================================================================
    // WeeklySummaryCommand Tests
    // =========================================================================

    @Test
    @DisplayName("WeeklySummaryCommand success displays weekly progress with daily moods")
    void weeklySummaryCommand_Success_DisplaysProgressWithDailyMoods() {
        UUID userId = UUID.randomUUID();
        Supplier<UUID> userSupplier = () -> userId;

        WeeklyProgress.DailyMood monday = WeeklyProgress.DailyMood.builder()
                .date(LocalDate.of(2026, 2, 23))
                .averageMood(7.5)
                .entriesCount(2)
                .build();

        WeeklyProgress.DailyMood tuesday = WeeklyProgress.DailyMood.builder()
                .date(LocalDate.of(2026, 2, 24))
                .averageMood(8.0)
                .entriesCount(1)
                .build();

        WeeklyProgress progress = WeeklyProgress.builder()
                .weekStart(LocalDate.of(2026, 2, 23))
                .weekEnd(LocalDate.of(2026, 3, 1))
                .sessionsCompleted(5)
                .diaryEntries(10)
                .averageMood(7.3)
                .streakDays(12)
                .dailyMoods(List.of(monday, tuesday))
                .build();

        when(apiClient.get("/progress/weekly", WeeklyProgress.class)).thenReturn(progress);

        ProgressCommands.WeeklySummaryCommand command =
                new ProgressCommands.WeeklySummaryCommand(apiClient, userSupplier);

        command.execute();

        String output = capturedOutput();
        assertThat(output).contains("Weekly Progress Summary");
        assertThat(output).contains("2026-02-23");
        assertThat(output).contains("2026-03-01");
        assertThat(output).contains("5");
        assertThat(output).contains("10");
        assertThat(output).contains("7.3");
        assertThat(output).contains("12");
        assertThat(output).contains("Daily Mood Breakdown");
        assertThat(output).contains("2026-02-23");
        assertThat(output).contains("7.5");
        assertThat(output).contains("2026-02-24");
        assertThat(output).contains("8.0");
    }

    @Test
    @DisplayName("WeeklySummaryCommand when progress is null prints no data message")
    void weeklySummaryCommand_NullProgress_PrintsNoDataMessage() {
        UUID userId = UUID.randomUUID();
        Supplier<UUID> userSupplier = () -> userId;

        when(apiClient.get("/progress/weekly", WeeklyProgress.class)).thenReturn(null);

        ProgressCommands.WeeklySummaryCommand command =
                new ProgressCommands.WeeklySummaryCommand(apiClient, userSupplier);

        command.execute();

        assertThat(capturedOutput()).contains("No weekly progress data available yet.");
    }

    @Test
    @DisplayName("WeeklySummaryCommand when API throws exception prints error message")
    void weeklySummaryCommand_ApiError_PrintsErrorMessage() {
        UUID userId = UUID.randomUUID();
        Supplier<UUID> userSupplier = () -> userId;

        when(apiClient.get("/progress/weekly", WeeklyProgress.class))
                .thenThrow(new RuntimeException("Connection refused"));

        ProgressCommands.WeeklySummaryCommand command =
                new ProgressCommands.WeeklySummaryCommand(apiClient, userSupplier);

        command.execute();

        String output = capturedOutput();
        assertThat(output).contains("Failed to load weekly progress");
        assertThat(output).contains("Connection refused");
    }

    @Test
    @DisplayName("WeeklySummaryCommand getName returns Weekly Summary")
    void weeklySummaryCommand_GetName_ReturnsWeeklySummary() {
        Supplier<UUID> userSupplier = UUID::randomUUID;

        ProgressCommands.WeeklySummaryCommand command =
                new ProgressCommands.WeeklySummaryCommand(apiClient, userSupplier);

        assertThat(command.getName()).isEqualTo("Weekly Summary");
    }

    // =========================================================================
    // MonthlyTrendsCommand Tests
    // =========================================================================

    @Test
    @DisplayName("MonthlyTrendsCommand success displays monthly trend with weekly breakdown")
    void monthlyTrendsCommand_Success_DisplaysTrendWithWeeks() {
        UUID userId = UUID.randomUUID();
        Supplier<UUID> userSupplier = () -> userId;

        MonthlyTrend.WeeklySummaryItem week1 = MonthlyTrend.WeeklySummaryItem.builder()
                .weekNumber(1)
                .sessions(3)
                .entries(5)
                .avgMood(6.5)
                .build();

        MonthlyTrend.WeeklySummaryItem week2 = MonthlyTrend.WeeklySummaryItem.builder()
                .weekNumber(2)
                .sessions(4)
                .entries(7)
                .avgMood(7.2)
                .build();

        MonthlyTrend trend = MonthlyTrend.builder()
                .month("February")
                .year(2026)
                .totalSessions(12)
                .totalDiaryEntries(20)
                .averageMoodStart(5.5)
                .averageMoodEnd(7.8)
                .moodTrend(2.3)
                .weeks(List.of(week1, week2))
                .build();

        when(apiClient.get("/progress/monthly", MonthlyTrend.class)).thenReturn(trend);

        ProgressCommands.MonthlyTrendsCommand command =
                new ProgressCommands.MonthlyTrendsCommand(apiClient, userSupplier);

        command.execute();

        String output = capturedOutput();
        assertThat(output).contains("Monthly Trends");
        assertThat(output).contains("February");
        assertThat(output).contains("2026");
        assertThat(output).contains("12");
        assertThat(output).contains("20");
        assertThat(output).contains("5.5");
        assertThat(output).contains("7.8");
        assertThat(output).contains("+2.3");
        assertThat(output).contains("Weekly Breakdown");
        assertThat(output).contains("6.5");
        assertThat(output).contains("7.2");
    }

    @Test
    @DisplayName("MonthlyTrendsCommand when trend is null prints no data message")
    void monthlyTrendsCommand_NullTrend_PrintsNoDataMessage() {
        UUID userId = UUID.randomUUID();
        Supplier<UUID> userSupplier = () -> userId;

        when(apiClient.get("/progress/monthly", MonthlyTrend.class)).thenReturn(null);

        ProgressCommands.MonthlyTrendsCommand command =
                new ProgressCommands.MonthlyTrendsCommand(apiClient, userSupplier);

        command.execute();

        assertThat(capturedOutput()).contains("No monthly trend data available yet.");
    }

    @Test
    @DisplayName("MonthlyTrendsCommand when API throws exception prints error message")
    void monthlyTrendsCommand_ApiError_PrintsErrorMessage() {
        UUID userId = UUID.randomUUID();
        Supplier<UUID> userSupplier = () -> userId;

        when(apiClient.get("/progress/monthly", MonthlyTrend.class))
                .thenThrow(new RuntimeException("Server error"));

        ProgressCommands.MonthlyTrendsCommand command =
                new ProgressCommands.MonthlyTrendsCommand(apiClient, userSupplier);

        command.execute();

        String output = capturedOutput();
        assertThat(output).contains("Failed to load monthly trends");
        assertThat(output).contains("Server error");
    }

    @Test
    @DisplayName("MonthlyTrendsCommand getName returns Monthly Trends")
    void monthlyTrendsCommand_GetName_ReturnsMonthlyTrends() {
        Supplier<UUID> userSupplier = UUID::randomUUID;

        ProgressCommands.MonthlyTrendsCommand command =
                new ProgressCommands.MonthlyTrendsCommand(apiClient, userSupplier);

        assertThat(command.getName()).isEqualTo("Monthly Trends");
    }

    // =========================================================================
    // AchievementsCommand Tests
    // =========================================================================

    @Test
    @DisplayName("AchievementsCommand success displays mixed unlocked and locked achievements")
    @SuppressWarnings("unchecked")
    void achievementsCommand_Success_DisplaysMixedAchievements() {
        UUID userId = UUID.randomUUID();
        Supplier<UUID> userSupplier = () -> userId;

        Achievement unlocked = Achievement.builder()
                .id("ach-1")
                .name("First Session")
                .description("Complete your first therapy session")
                .icon("*")
                .unlocked(true)
                .progress(1.0)
                .unlockedAt(LocalDateTime.of(2026, 2, 15, 14, 30))
                .build();

        Achievement locked = Achievement.builder()
                .id("ach-2")
                .name("Week Warrior")
                .description("Complete 7 sessions in one week")
                .icon("#")
                .unlocked(false)
                .progress(0.43)
                .unlockedAt(null)
                .build();

        when(apiClient.get(eq("/progress/achievements"), any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(unlocked, locked));

        ProgressCommands.AchievementsCommand command =
                new ProgressCommands.AchievementsCommand(apiClient, userSupplier);

        command.execute();

        String output = capturedOutput();
        assertThat(output).contains("Achievements");
        assertThat(output).contains("First Session");
        assertThat(output).contains("Complete your first therapy session");
        assertThat(output).contains("[UNLOCKED]");
        assertThat(output).contains("2026-02-15 14:30");
        assertThat(output).contains("Week Warrior");
        assertThat(output).contains("Complete 7 sessions in one week");
        assertThat(output).contains("[43%]");
        assertThat(output).contains("1/2 achievements unlocked");
    }

    @Test
    @DisplayName("AchievementsCommand with empty list prints no achievements message")
    @SuppressWarnings("unchecked")
    void achievementsCommand_EmptyList_PrintsNoAchievementsMessage() {
        UUID userId = UUID.randomUUID();
        Supplier<UUID> userSupplier = () -> userId;

        when(apiClient.get(eq("/progress/achievements"), any(ParameterizedTypeReference.class)))
                .thenReturn(Collections.emptyList());

        ProgressCommands.AchievementsCommand command =
                new ProgressCommands.AchievementsCommand(apiClient, userSupplier);

        command.execute();

        assertThat(capturedOutput()).contains("No achievements yet. Keep going!");
    }

    @Test
    @DisplayName("AchievementsCommand when API throws exception prints error message")
    @SuppressWarnings("unchecked")
    void achievementsCommand_ApiError_PrintsErrorMessage() {
        UUID userId = UUID.randomUUID();
        Supplier<UUID> userSupplier = () -> userId;

        when(apiClient.get(eq("/progress/achievements"), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Timeout"));

        ProgressCommands.AchievementsCommand command =
                new ProgressCommands.AchievementsCommand(apiClient, userSupplier);

        command.execute();

        String output = capturedOutput();
        assertThat(output).contains("Failed to load achievements");
        assertThat(output).contains("Timeout");
    }

    @Test
    @DisplayName("AchievementsCommand getName returns Achievements")
    void achievementsCommand_GetName_ReturnsAchievements() {
        Supplier<UUID> userSupplier = UUID::randomUUID;

        ProgressCommands.AchievementsCommand command =
                new ProgressCommands.AchievementsCommand(apiClient, userSupplier);

        assertThat(command.getName()).isEqualTo("Achievements");
    }

    // =========================================================================
    // ProgressMenuCommand — Logged In Path
    // =========================================================================

    @Test
    @DisplayName("ProgressMenuCommand when logged in displays sub-menu and selects Back")
    void progressMenuCommand_LoggedIn_DisplaysSubMenu() {
        UUID userId = UUID.randomUUID();
        // Input: "4" selects Back (WeeklySummary, MonthlyTrends, Achievements, Back)
        Scanner scanner = new Scanner(new java.io.ByteArrayInputStream("4\n".getBytes()));
        ProgressCommands.ProgressMenuCommand command = new ProgressCommands.ProgressMenuCommand(
                scanner, apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Progress Dashboard");
        assertThat(output).contains("Back to Main Menu");
    }

    @Test
    @DisplayName("ProgressMenuCommand getDescription returns non-blank")
    void progressMenuCommand_GetDescription_ReturnsDescription() {
        Scanner scanner = new Scanner(new java.io.ByteArrayInputStream("".getBytes()));
        ProgressCommands.ProgressMenuCommand command = new ProgressCommands.ProgressMenuCommand(
                scanner, apiClient, () -> null);

        assertThat(command.getDescription()).isNotBlank();
    }
}

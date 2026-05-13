package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.ApiClient;
import com.digitaltherapy.cli.RestPage;
import com.digitaltherapy.dto.DiaryEntryCreate;
import com.digitaltherapy.dto.DiaryEntryResponse;
import com.digitaltherapy.dto.DiaryEntrySummary;
import com.digitaltherapy.dto.DiaryInsights;
import com.digitaltherapy.dto.DistortionSuggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryCommandsTest {

    @Mock
    private ApiClient apiClient;

    @Mock
    private RestPage<DiaryEntrySummary> mockPage;

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

    private Scanner scannerWithInput(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    // =========================================================================
    // DiaryMenuCommand Tests
    // =========================================================================

    @Test
    @DisplayName("DiaryMenuCommand when not logged in prints login message")
    void diaryMenuCommand_NotLoggedIn_PrintsLoginMessage() {
        Scanner scanner = scannerWithInput("");
        DiaryCommands.DiaryMenuCommand command = new DiaryCommands.DiaryMenuCommand(
                scanner, apiClient, () -> null);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Please log in first to access the Thought Diary.");
    }

    @Test
    @DisplayName("DiaryMenuCommand getName returns Thought Diary")
    void diaryMenuCommand_GetName_ReturnsThoughtDiary() {
        Scanner scanner = scannerWithInput("");
        DiaryCommands.DiaryMenuCommand command = new DiaryCommands.DiaryMenuCommand(
                scanner, apiClient, () -> null);

        assertThat(command.getName()).isEqualTo("Thought Diary");
    }

    @Test
    @DisplayName("DiaryMenuCommand getDescription returns non-blank description")
    void diaryMenuCommand_GetDescription_ReturnsDescription() {
        Scanner scanner = scannerWithInput("");
        DiaryCommands.DiaryMenuCommand command = new DiaryCommands.DiaryMenuCommand(
                scanner, apiClient, () -> null);

        assertThat(command.getDescription()).isNotBlank();
    }

    // =========================================================================
    // NewEntryCommand Tests
    // =========================================================================

    @Test
    @DisplayName("NewEntryCommand successful creation prints success and suggests distortions")
    void newEntryCommand_SuccessfulCreation() {
        UUID userId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        DiaryEntryResponse response = DiaryEntryResponse.builder()
                .id(entryId)
                .situation("Meeting at work")
                .automaticThought("Everyone thinks I am incompetent")
                .moodBefore(5)
                .moodAfter(7)
                .build();

        when(apiClient.post(eq("/diary/entries"), any(DiaryEntryCreate.class), eq(DiaryEntryResponse.class)))
                .thenReturn(response);

        List<DistortionSuggestion> suggestions = List.of(
                DistortionSuggestion.builder()
                        .distortionId("mind-reading")
                        .name("Mind Reading")
                        .confidence(0.85)
                        .reasoning("Assuming what others think without evidence")
                        .build()
        );

        when(apiClient.post(eq("/diary/distortions/suggest"), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(suggestions);

        String input = "Meeting at work\nEveryone thinks I am incompetent\nanxiety\n7\ndone\n5\n7\nMaybe they were just busy\n";
        Scanner scanner = scannerWithInput(input);
        DiaryCommands.NewEntryCommand command = new DiaryCommands.NewEntryCommand(
                scanner, apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Diary entry created successfully!");
        assertThat(output).contains("Entry ID: " + entryId);
        assertThat(output).contains("Mind Reading");
        assertThat(output).contains("85%");
        assertThat(output).contains("Assuming what others think without evidence");

        verify(apiClient).post(eq("/diary/entries"), any(DiaryEntryCreate.class), eq(DiaryEntryResponse.class));
        verify(apiClient).post(eq("/diary/distortions/suggest"), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("NewEntryCommand with empty situation shows error")
    void newEntryCommand_EmptySituation_ShowsError() {
        UUID userId = UUID.randomUUID();

        String input = "\nSome thought\n";
        Scanner scanner = scannerWithInput(input);
        DiaryCommands.NewEntryCommand command = new DiaryCommands.NewEntryCommand(
                scanner, apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Situation cannot be empty.");
    }

    @Test
    @DisplayName("NewEntryCommand with empty automatic thought shows error")
    void newEntryCommand_EmptyThought_ShowsError() {
        UUID userId = UUID.randomUUID();

        String input = "Meeting at work\n\n";
        Scanner scanner = scannerWithInput(input);
        DiaryCommands.NewEntryCommand command = new DiaryCommands.NewEntryCommand(
                scanner, apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Automatic thought cannot be empty.");
    }

    @Test
    @DisplayName("NewEntryCommand when API throws exception shows error message")
    void newEntryCommand_ApiError_ShowsError() {
        UUID userId = UUID.randomUUID();

        when(apiClient.post(eq("/diary/entries"), any(DiaryEntryCreate.class), eq(DiaryEntryResponse.class)))
                .thenThrow(new RuntimeException("Server error"));

        String input = "Meeting at work\nEveryone thinks I am incompetent\ndone\n5\n7\nAlternative thought\n";
        Scanner scanner = scannerWithInput(input);
        DiaryCommands.NewEntryCommand command = new DiaryCommands.NewEntryCommand(
                scanner, apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Failed to create diary entry");
        assertThat(output).contains("Server error");
    }

    @Test
    @DisplayName("NewEntryCommand getName returns New Diary Entry")
    void newEntryCommand_GetName_ReturnsNewDiaryEntry() {
        Scanner scanner = scannerWithInput("");
        DiaryCommands.NewEntryCommand command = new DiaryCommands.NewEntryCommand(
                scanner, apiClient, () -> null);

        assertThat(command.getName()).isEqualTo("New Diary Entry");
    }

    // =========================================================================
    // ViewEntriesCommand Tests
    // =========================================================================

    @Test
    @DisplayName("ViewEntriesCommand displays entries and navigates back")
    void viewEntriesCommand_PageWithEntries_DisplaysAndGoesBack() {
        UUID userId = UUID.randomUUID();

        DiaryEntrySummary summary = DiaryEntrySummary.builder()
                .id(UUID.randomUUID())
                .situation("Meeting at work")
                .automaticThought("I will fail")
                .moodBefore(4)
                .moodAfter(6)
                .distortionCount(2)
                .createdAt(LocalDateTime.of(2026, 3, 1, 10, 30))
                .build();

        when(mockPage.isEmpty()).thenReturn(false);
        when(mockPage.getTotalPages()).thenReturn(1);
        when(mockPage.getContent()).thenReturn(List.of(summary));
        when(mockPage.getNumberOfElements()).thenReturn(1);
        when(mockPage.getTotalElements()).thenReturn(1L);
        when(mockPage.hasPrevious()).thenReturn(false);
        when(mockPage.hasNext()).thenReturn(false);

        when(apiClient.get(eq("/diary/entries?page=0&size=5&sort=createdAt,desc"),
                any(ParameterizedTypeReference.class))).thenReturn(mockPage);

        Scanner scanner = scannerWithInput("B\n");
        DiaryCommands.ViewEntriesCommand command = new DiaryCommands.ViewEntriesCommand(
                scanner, apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Diary Entries (Page 1 of 1)");
        assertThat(output).contains("Meeting at work");
        assertThat(output).contains("I will fail");
        assertThat(output).contains("Mood: 4 -> 6");
        assertThat(output).contains("Distortions: 2");
        assertThat(output).contains("Showing 1 of 1 total entries");
        assertThat(output).contains("[B]ack");
    }

    @Test
    @DisplayName("ViewEntriesCommand with empty first page shows no entries message")
    void viewEntriesCommand_EmptyFirstPage_ShowsNoEntries() {
        UUID userId = UUID.randomUUID();

        when(mockPage.isEmpty()).thenReturn(true);

        when(apiClient.get(eq("/diary/entries?page=0&size=5&sort=createdAt,desc"),
                any(ParameterizedTypeReference.class))).thenReturn(mockPage);

        Scanner scanner = scannerWithInput("");
        DiaryCommands.ViewEntriesCommand command = new DiaryCommands.ViewEntriesCommand(
                scanner, apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("No diary entries found. Create your first entry!");
    }

    @Test
    @DisplayName("ViewEntriesCommand getName returns View Entries")
    void viewEntriesCommand_GetName_ReturnsViewEntries() {
        Scanner scanner = scannerWithInput("");
        DiaryCommands.ViewEntriesCommand command = new DiaryCommands.ViewEntriesCommand(
                scanner, apiClient, () -> null);

        assertThat(command.getName()).isEqualTo("View Entries");
    }

    // =========================================================================
    // ViewInsightsCommand Tests
    // =========================================================================

    @Test
    @DisplayName("ViewInsightsCommand displays all insight fields")
    void viewInsightsCommand_SuccessWithAllFields() {
        UUID userId = UUID.randomUUID();

        DiaryInsights insights = DiaryInsights.builder()
                .totalEntries(15)
                .averageMoodImprovement(2.5)
                .topDistortions(List.of(
                        DiaryInsights.DistortionFrequency.builder()
                                .distortionId("catastrophizing")
                                .name("Catastrophizing")
                                .count(8)
                                .build(),
                        DiaryInsights.DistortionFrequency.builder()
                                .distortionId("mind-reading")
                                .name("Mind Reading")
                                .count(5)
                                .build()
                ))
                .patterns(List.of("Negative thoughts peak in the evening", "Work-related stress is a common trigger"))
                .recommendations(List.of("Practice mindfulness before meetings", "Challenge catastrophic thoughts"))
                .build();

        when(apiClient.get("/diary/insights", DiaryInsights.class)).thenReturn(insights);

        DiaryCommands.ViewInsightsCommand command = new DiaryCommands.ViewInsightsCommand(
                apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Diary Insights");
        assertThat(output).contains("Total Entries:");
        assertThat(output).contains("15");
        assertThat(output).contains("Avg Mood Improvement:");
        assertThat(output).contains("2.5");
        assertThat(output).contains("Catastrophizing");
        assertThat(output).contains("8 occurrences");
        assertThat(output).contains("Mind Reading");
        assertThat(output).contains("5 occurrences");
        assertThat(output).contains("Negative thoughts peak in the evening");
        assertThat(output).contains("Work-related stress is a common trigger");
        assertThat(output).contains("Practice mindfulness before meetings");
        assertThat(output).contains("Challenge catastrophic thoughts");

        verify(apiClient).get("/diary/insights", DiaryInsights.class);
    }

    @Test
    @DisplayName("ViewInsightsCommand with null insights shows no insights message")
    void viewInsightsCommand_NullInsights_ShowsNoInsightsMessage() {
        UUID userId = UUID.randomUUID();

        when(apiClient.get("/diary/insights", DiaryInsights.class)).thenReturn(null);

        DiaryCommands.ViewInsightsCommand command = new DiaryCommands.ViewInsightsCommand(
                apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("No insights available yet. Keep journaling!");
    }

    @Test
    @DisplayName("ViewInsightsCommand when API throws exception shows error message")
    void viewInsightsCommand_ApiError_ShowsError() {
        UUID userId = UUID.randomUUID();

        when(apiClient.get("/diary/insights", DiaryInsights.class))
                .thenThrow(new RuntimeException("Connection refused"));

        DiaryCommands.ViewInsightsCommand command = new DiaryCommands.ViewInsightsCommand(
                apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Failed to load insights");
        assertThat(output).contains("Connection refused");
    }

    @Test
    @DisplayName("ViewInsightsCommand getName returns View Insights")
    void viewInsightsCommand_GetName_ReturnsViewInsights() {
        DiaryCommands.ViewInsightsCommand command = new DiaryCommands.ViewInsightsCommand(
                apiClient, () -> null);

        assertThat(command.getName()).isEqualTo("View Insights");
    }

    @Test
    @DisplayName("ViewInsightsCommand getDescription returns non-blank description")
    void viewInsightsCommand_GetDescription_ReturnsDescription() {
        DiaryCommands.ViewInsightsCommand command = new DiaryCommands.ViewInsightsCommand(
                apiClient, () -> null);

        assertThat(command.getDescription()).isNotBlank();
    }

    // =========================================================================
    // DiaryMenuCommand — Logged In Path
    // =========================================================================

    @Test
    @DisplayName("DiaryMenuCommand when logged in displays sub-menu and selects Back")
    void diaryMenuCommand_LoggedIn_DisplaysSubMenu() {
        UUID userId = UUID.randomUUID();
        // Input: "4" selects Back (NewEntry, ViewEntries, ViewInsights, Back)
        Scanner scanner = scannerWithInput("4\n");
        DiaryCommands.DiaryMenuCommand command = new DiaryCommands.DiaryMenuCommand(
                scanner, apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Thought Diary");
        assertThat(output).contains("Back to Main Menu");
    }

}

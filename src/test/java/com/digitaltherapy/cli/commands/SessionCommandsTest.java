package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.ApiClient;
import com.digitaltherapy.dto.*;
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
class SessionCommandsTest {

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

    private Scanner scannerWithInput(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    // =========================================================================
    // SessionMenuCommand Tests
    // =========================================================================

    @Test
    @DisplayName("SessionMenuCommand when not logged in prints login message")
    void sessionMenuCommand_NotLoggedIn_PrintsLoginMessage() {
        Scanner scanner = scannerWithInput("");
        SessionCommands.SessionMenuCommand command = new SessionCommands.SessionMenuCommand(
                scanner, apiClient, () -> null);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Please log in first to access CBT Sessions.");
    }

    @Test
    @DisplayName("SessionMenuCommand getName returns CBT Sessions")
    void sessionMenuCommand_GetName_ReturnsCBTSessions() {
        Scanner scanner = scannerWithInput("");
        SessionCommands.SessionMenuCommand command = new SessionCommands.SessionMenuCommand(
                scanner, apiClient, () -> null);

        assertThat(command.getName()).isEqualTo("CBT Sessions");
    }

    // =========================================================================
    // ViewSessionLibraryCommand Tests
    // =========================================================================

    @Test
    @DisplayName("ViewSessionLibraryCommand displays modules and sessions on success")
    void viewSessionLibraryCommand_Success_ShowsModules() {
        UUID moduleId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        SessionModuleDto.SessionSummaryItem sessionItem = SessionModuleDto.SessionSummaryItem.builder()
                .id(sessionId)
                .title("Introduction to CBT")
                .durationMinutes(30)
                .build();

        SessionModuleDto module = SessionModuleDto.builder()
                .id(moduleId)
                .name("Core CBT")
                .description("Foundational CBT techniques")
                .category("Foundation")
                .orderIndex(1)
                .sessions(List.of(sessionItem))
                .build();

        when(apiClient.get(eq("/sessions"), any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(module));

        Scanner scanner = scannerWithInput("");
        SessionCommands.ViewSessionLibraryCommand command = new SessionCommands.ViewSessionLibraryCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Session Library");
        assertThat(output).contains("Module: Core CBT");
        assertThat(output).contains("Category: Foundation");
        assertThat(output).contains("Foundational CBT techniques");
        assertThat(output).contains("Introduction to CBT");
        assertThat(output).contains("30 min");

        verify(apiClient).get(eq("/sessions"), any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("ViewSessionLibraryCommand with empty list prints no modules message")
    void viewSessionLibraryCommand_EmptyList_PrintsNoModules() {
        when(apiClient.get(eq("/sessions"), any(ParameterizedTypeReference.class)))
                .thenReturn(Collections.emptyList());

        Scanner scanner = scannerWithInput("");
        SessionCommands.ViewSessionLibraryCommand command = new SessionCommands.ViewSessionLibraryCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("No session modules available");
    }

    @Test
    @DisplayName("ViewSessionLibraryCommand when API throws exception shows error message")
    void viewSessionLibraryCommand_ApiError_ShowsError() {
        when(apiClient.get(eq("/sessions"), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        Scanner scanner = scannerWithInput("");
        SessionCommands.ViewSessionLibraryCommand command = new SessionCommands.ViewSessionLibraryCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Failed to load session library");
        assertThat(output).contains("Connection refused");
    }

    @Test
    @DisplayName("ViewSessionLibraryCommand getName returns View Session Library")
    void viewSessionLibraryCommand_GetName_ReturnsViewSessionLibrary() {
        Scanner scanner = scannerWithInput("");
        SessionCommands.ViewSessionLibraryCommand command = new SessionCommands.ViewSessionLibraryCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        assertThat(command.getName()).isEqualTo("View Session Library");
    }

    @Test
    @DisplayName("ViewSessionLibraryCommand getDescription returns non-blank description")
    void viewSessionLibraryCommand_GetDescription_ReturnsDescription() {
        Scanner scanner = scannerWithInput("");
        SessionCommands.ViewSessionLibraryCommand command = new SessionCommands.ViewSessionLibraryCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        assertThat(command.getDescription()).isNotBlank();
    }

    // =========================================================================
    // StartNewSessionCommand Tests
    // =========================================================================

    @Test
    @DisplayName("StartNewSessionCommand full chat flow selects session chats and exits")
    void startNewSessionCommand_FullChatFlow_SelectsChatAndExits() {
        UUID sessionId = UUID.randomUUID();
        UUID userSessionId = UUID.randomUUID();

        SessionModuleDto.SessionSummaryItem sessionItem = SessionModuleDto.SessionSummaryItem.builder()
                .id(sessionId)
                .title("Thought Challenging")
                .durationMinutes(25)
                .build();

        SessionModuleDto module = SessionModuleDto.builder()
                .id(UUID.randomUUID())
                .name("Core CBT")
                .description("Core techniques")
                .category("Foundation")
                .orderIndex(1)
                .sessions(List.of(sessionItem))
                .build();

        ActiveSession activeSession = ActiveSession.builder()
                .sessionId(sessionId)
                .userSessionId(userSessionId)
                .title("Thought Challenging")
                .description("Learn to challenge negative thoughts")
                .startedAt(LocalDateTime.now())
                .moodBefore(5)
                .build();

        ChatResponse chatResponse = ChatResponse.builder()
                .message("Welcome! How are you feeling today?")
                .role("THERAPIST")
                .timestamp(LocalDateTime.now())
                .crisisDetected(false)
                .build();

        SessionSummary sessionSummary = SessionSummary.builder()
                .sessionId(sessionId)
                .title("Thought Challenging")
                .status("COMPLETED")
                .startedAt(LocalDateTime.now().minusMinutes(25))
                .endedAt(LocalDateTime.now())
                .moodBefore(5)
                .moodAfter(7)
                .summary("Great progress on identifying thought patterns.")
                .keyInsights(List.of("Identified catastrophizing pattern", "Practiced reframing"))
                .build();

        when(apiClient.get(eq("/sessions"), any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(module));
        when(apiClient.post(eq("/sessions/" + sessionId + "/start"), any(), eq(ActiveSession.class)))
                .thenReturn(activeSession);
        when(apiClient.post(eq("/sessions/" + userSessionId + "/chat"), any(ChatRequest.class), eq(ChatResponse.class)))
                .thenReturn(chatResponse);
        when(apiClient.post(eq("/sessions/" + userSessionId + "/end"), any(), eq(SessionSummary.class)))
                .thenReturn(sessionSummary);

        // User selects session 1, sends a message, then types exit
        Scanner scanner = scannerWithInput("1\nI feel anxious today\nexit\n");
        SessionCommands.StartNewSessionCommand command = new SessionCommands.StartNewSessionCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Session Started: Thought Challenging");
        assertThat(output).contains("Learn to challenge negative thoughts");
        assertThat(output).contains("Welcome! How are you feeling today?");
        assertThat(output).contains("Session Ended");
        assertThat(output).contains("Great progress on identifying thought patterns.");
        assertThat(output).contains("Mood: 5 -> 7");
        assertThat(output).contains("Identified catastrophizing pattern");
        assertThat(output).contains("Practiced reframing");

        verify(apiClient).post(eq("/sessions/" + sessionId + "/start"), any(), eq(ActiveSession.class));
        verify(apiClient).post(eq("/sessions/" + userSessionId + "/chat"), any(ChatRequest.class), eq(ChatResponse.class));
        verify(apiClient).post(eq("/sessions/" + userSessionId + "/end"), any(), eq(SessionSummary.class));
    }

    @Test
    @DisplayName("StartNewSessionCommand cancel with 0 returns without starting session")
    void startNewSessionCommand_CancelWithZero_Returns() {
        UUID sessionId = UUID.randomUUID();

        SessionModuleDto.SessionSummaryItem sessionItem = SessionModuleDto.SessionSummaryItem.builder()
                .id(sessionId)
                .title("Thought Challenging")
                .durationMinutes(25)
                .build();

        SessionModuleDto module = SessionModuleDto.builder()
                .id(UUID.randomUUID())
                .name("Core CBT")
                .description("Core techniques")
                .category("Foundation")
                .orderIndex(1)
                .sessions(List.of(sessionItem))
                .build();

        when(apiClient.get(eq("/sessions"), any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(module));

        Scanner scanner = scannerWithInput("0\n");
        SessionCommands.StartNewSessionCommand command = new SessionCommands.StartNewSessionCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Available Sessions");
        assertThat(output).contains("Cancel");
        assertThat(output).doesNotContain("Session Started");
    }

    @Test
    @DisplayName("StartNewSessionCommand invalid input abc shows error message")
    void startNewSessionCommand_InvalidInput_ShowsError() {
        UUID sessionId = UUID.randomUUID();

        SessionModuleDto.SessionSummaryItem sessionItem = SessionModuleDto.SessionSummaryItem.builder()
                .id(sessionId)
                .title("Thought Challenging")
                .durationMinutes(25)
                .build();

        SessionModuleDto module = SessionModuleDto.builder()
                .id(UUID.randomUUID())
                .name("Core CBT")
                .description("Core techniques")
                .category("Foundation")
                .orderIndex(1)
                .sessions(List.of(sessionItem))
                .build();

        when(apiClient.get(eq("/sessions"), any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(module));

        Scanner scanner = scannerWithInput("abc\n");
        SessionCommands.StartNewSessionCommand command = new SessionCommands.StartNewSessionCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Please enter a valid number");
    }

    @Test
    @DisplayName("StartNewSessionCommand with empty modules prints no modules message")
    void startNewSessionCommand_EmptyModules_PrintsNoModules() {
        when(apiClient.get(eq("/sessions"), any(ParameterizedTypeReference.class)))
                .thenReturn(Collections.emptyList());

        Scanner scanner = scannerWithInput("");
        SessionCommands.StartNewSessionCommand command = new SessionCommands.StartNewSessionCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("No session modules available");
    }

    @Test
    @DisplayName("StartNewSessionCommand getName returns Start New Session")
    void startNewSessionCommand_GetName_ReturnsStartNewSession() {
        Scanner scanner = scannerWithInput("");
        SessionCommands.StartNewSessionCommand command = new SessionCommands.StartNewSessionCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        assertThat(command.getName()).isEqualTo("Start New Session");
    }

    // =========================================================================
    // ViewSessionHistoryCommand Tests
    // =========================================================================

    @Test
    @DisplayName("ViewSessionHistoryCommand displays history entries on success")
    void viewSessionHistoryCommand_Success_ShowsEntries() {
        SessionHistoryEntry entry = SessionHistoryEntry.builder()
                .id(UUID.randomUUID())
                .sessionTitle("Thought Challenging")
                .moduleName("Core CBT")
                .status("COMPLETED")
                .startedAt(LocalDateTime.of(2025, 3, 15, 10, 30))
                .endedAt(LocalDateTime.of(2025, 3, 15, 11, 0))
                .moodBefore(4)
                .moodAfter(7)
                .build();

        when(apiClient.get(eq("/sessions/history"), any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(entry));

        Scanner scanner = scannerWithInput("");
        SessionCommands.ViewSessionHistoryCommand command = new SessionCommands.ViewSessionHistoryCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Session History");
        assertThat(output).contains("Thought Challenging");
        assertThat(output).contains("Core CBT");
        assertThat(output).contains("COMPLETED");
        assertThat(output).contains("2025-03-15 10:30");
        assertThat(output).contains("2025-03-15 11:00");
        assertThat(output).contains("4 -> 7");

        verify(apiClient).get(eq("/sessions/history"), any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("ViewSessionHistoryCommand with empty history prints no history message")
    void viewSessionHistoryCommand_EmptyHistory_PrintsNoHistory() {
        when(apiClient.get(eq("/sessions/history"), any(ParameterizedTypeReference.class)))
                .thenReturn(Collections.emptyList());

        Scanner scanner = scannerWithInput("");
        SessionCommands.ViewSessionHistoryCommand command = new SessionCommands.ViewSessionHistoryCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("No session history found");
    }

    @Test
    @DisplayName("ViewSessionHistoryCommand when API throws exception shows error message")
    void viewSessionHistoryCommand_ApiError_ShowsError() {
        when(apiClient.get(eq("/sessions/history"), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Server unavailable"));

        Scanner scanner = scannerWithInput("");
        SessionCommands.ViewSessionHistoryCommand command = new SessionCommands.ViewSessionHistoryCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Failed to load session history");
        assertThat(output).contains("Server unavailable");
    }

    @Test
    @DisplayName("ViewSessionHistoryCommand getName returns View Session History")
    void viewSessionHistoryCommand_GetName_ReturnsViewSessionHistory() {
        Scanner scanner = scannerWithInput("");
        SessionCommands.ViewSessionHistoryCommand command = new SessionCommands.ViewSessionHistoryCommand(
                scanner, apiClient, () -> UUID.randomUUID());

        assertThat(command.getName()).isEqualTo("View Session History");
    }

    // =========================================================================
    // SessionMenuCommand — Logged In Path
    // =========================================================================

    @Test
    @DisplayName("SessionMenuCommand when logged in displays session sub-menu and selects Back")
    void sessionMenuCommand_LoggedIn_DisplaysSubMenu() {
        UUID userId = UUID.randomUUID();
        // Input: "4" selects Back (4th option: ViewLibrary, StartNew, ViewHistory, Back)
        Scanner scanner = scannerWithInput("4\n");
        SessionCommands.SessionMenuCommand command = new SessionCommands.SessionMenuCommand(
                scanner, apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("CBT Sessions");
        assertThat(output).contains("Back to Main Menu");
    }

    @Test
    @DisplayName("SessionMenuCommand getDescription returns non-blank")
    void sessionMenuCommand_GetDescription_ReturnsDescription() {
        Scanner scanner = scannerWithInput("");
        SessionCommands.SessionMenuCommand command = new SessionCommands.SessionMenuCommand(
                scanner, apiClient, () -> null);

        assertThat(command.getDescription()).isNotBlank();
    }
}

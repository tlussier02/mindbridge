package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.ApiClient;
import com.digitaltherapy.dto.WeeklyProgress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsCommandTest {

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
    // execute Tests
    // =========================================================================

    @Test
    @DisplayName("execute when logged in shows profile info and streak days")
    void execute_LoggedIn_ShowsProfileAndStreak() {
        UUID userId = UUID.randomUUID();
        String userName = "Alice Johnson";

        WeeklyProgress progress = WeeklyProgress.builder()
                .streakDays(14)
                .build();

        when(apiClient.get("/progress/weekly", WeeklyProgress.class)).thenReturn(progress);

        SettingsCommand command = new SettingsCommand(
                () -> userId,
                () -> userName,
                apiClient
        );

        command.execute();

        String output = capturedOutput();
        assertThat(output).contains("Alice Johnson");
        assertThat(output).contains(userId.toString());
        assertThat(output).contains("Logged in");
        assertThat(output).contains("14 days");
        assertThat(output).contains("Digital Therapy Assistant");
        assertThat(output).contains("Version: 1.0.0");
    }

    @Test
    @DisplayName("execute when not logged in shows not-logged-in status and login prompt")
    void execute_NotLoggedIn_ShowsNotLoggedIn() {
        SettingsCommand command = new SettingsCommand(
                () -> null,
                () -> null,
                apiClient
        );

        command.execute();

        String output = capturedOutput();
        assertThat(output).contains("Not logged in");
        assertThat(output).contains("Please log in to view your profile.");
        assertThat(output).contains("Digital Therapy Assistant");
        assertThat(output).contains("Version: 1.0.0");
    }

    @Test
    @DisplayName("execute when progress API fails shows unable to load streak")
    void execute_ProgressApiFails_ShowsUnableToLoad() {
        UUID userId = UUID.randomUUID();
        String userName = "Bob Smith";

        when(apiClient.get("/progress/weekly", WeeklyProgress.class))
                .thenThrow(new RuntimeException("Service unavailable"));

        SettingsCommand command = new SettingsCommand(
                () -> userId,
                () -> userName,
                apiClient
        );

        command.execute();

        String output = capturedOutput();
        assertThat(output).contains("Bob Smith");
        assertThat(output).contains(userId.toString());
        assertThat(output).contains("Logged in");
        assertThat(output).contains("Unable to load");
        assertThat(output).contains("Digital Therapy Assistant");
        assertThat(output).contains("Version: 1.0.0");
    }

    // =========================================================================
    // getName / getDescription Tests
    // =========================================================================

    @Test
    @DisplayName("getName returns Settings")
    void getName_ReturnsSettings() {
        SettingsCommand command = new SettingsCommand(
                () -> null,
                () -> null,
                apiClient
        );

        assertThat(command.getName()).isEqualTo("Settings");
    }

    @Test
    @DisplayName("getDescription returns a non-blank description")
    void getDescription_ReturnsNonBlank() {
        SettingsCommand command = new SettingsCommand(
                () -> null,
                () -> null,
                apiClient
        );

        assertThat(command.getDescription()).isNotBlank();
    }
}

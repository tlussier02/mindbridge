package com.digitaltherapy.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class MenuHandlerTest {

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

    private Command createTestCommand(String name, String description, Runnable action) {
        return new Command() {
            @Override
            public void execute() {
                action.run();
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescription() {
                return description;
            }
        };
    }

    @Test
    @DisplayName("handleInput executes correct command when valid selection is provided")
    void handleInput_ValidSelection_ExecutesCommand() {
        AtomicBoolean commandExecuted = new AtomicBoolean(false);
        Command testCommand = createTestCommand("Test Command", "A test", () -> commandExecuted.set(true));
        Command otherCommand = createTestCommand("Other Command", "Another test", () -> {});

        Scanner scanner = scannerWithInput("1\n");
        MenuHandler handler = new MenuHandler("Test Menu", scanner, List.of(testCommand, otherCommand));

        handler.handleInput();

        assertThat(commandExecuted.get()).isTrue();
    }

    @Test
    @DisplayName("handleInput executes second command when 2 is selected")
    void handleInput_SelectSecondOption_ExecutesSecondCommand() {
        AtomicBoolean firstExecuted = new AtomicBoolean(false);
        AtomicBoolean secondExecuted = new AtomicBoolean(false);
        Command first = createTestCommand("First", "First command", () -> firstExecuted.set(true));
        Command second = createTestCommand("Second", "Second command", () -> secondExecuted.set(true));

        Scanner scanner = scannerWithInput("2\n");
        MenuHandler handler = new MenuHandler("Test Menu", scanner, List.of(first, second));

        handler.handleInput();

        assertThat(firstExecuted.get()).isFalse();
        assertThat(secondExecuted.get()).isTrue();
    }

    @Test
    @DisplayName("handleInput prints error for invalid number selection")
    void handleInput_InvalidNumber_PrintsError() {
        Command testCommand = createTestCommand("Test", "A test", () -> {});

        Scanner scanner = scannerWithInput("99\n");
        MenuHandler handler = new MenuHandler("Test Menu", scanner, List.of(testCommand));

        handler.handleInput();

        String output = outputStream.toString();
        assertThat(output).contains("Invalid option");
    }

    @Test
    @DisplayName("handleInput prints error for zero selection")
    void handleInput_ZeroSelection_PrintsError() {
        Command testCommand = createTestCommand("Test", "A test", () -> {});

        Scanner scanner = scannerWithInput("0\n");
        MenuHandler handler = new MenuHandler("Test Menu", scanner, List.of(testCommand));

        handler.handleInput();

        String output = outputStream.toString();
        assertThat(output).contains("Invalid option");
    }

    @Test
    @DisplayName("handleInput prints error for non-numeric input")
    void handleInput_NonNumericInput_PrintsError() {
        Command testCommand = createTestCommand("Test", "A test", () -> {});

        Scanner scanner = scannerWithInput("abc\n");
        MenuHandler handler = new MenuHandler("Test Menu", scanner, List.of(testCommand));

        handler.handleInput();

        String output = outputStream.toString();
        assertThat(output).contains("Please enter a valid number");
    }

    @Test
    @DisplayName("display shows all command names in the menu")
    void display_ShowsAllCommands() {
        Command cmd1 = createTestCommand("Start Session", "Begin a session", () -> {});
        Command cmd2 = createTestCommand("View Diary", "View your diary", () -> {});
        Command cmd3 = createTestCommand("Exit", "Exit app", () -> {});

        Scanner scanner = scannerWithInput("");
        MenuHandler handler = new MenuHandler("Main Menu", scanner, List.of(cmd1, cmd2, cmd3));

        handler.display();

        String output = outputStream.toString();
        assertThat(output).contains("Main Menu");
        assertThat(output).contains("Start Session");
        assertThat(output).contains("View Diary");
        assertThat(output).contains("Exit");
        assertThat(output).contains("1.");
        assertThat(output).contains("2.");
        assertThat(output).contains("3.");
        assertThat(output).contains("Select an option:");
    }

    @Test
    @DisplayName("display shows menu title with decorative borders")
    void display_ShowsMenuTitleWithBorders() {
        Command cmd = createTestCommand("Test", "A test", () -> {});

        Scanner scanner = scannerWithInput("");
        MenuHandler handler = new MenuHandler("My Menu", scanner, List.of(cmd));

        handler.display();

        String output = outputStream.toString();
        assertThat(output).contains("========================================");
        assertThat(output).contains("My Menu");
    }

    @Test
    @DisplayName("handleInput prints error for negative number selection")
    void handleInput_NegativeNumber_PrintsError() {
        Command testCommand = createTestCommand("Test", "A test", () -> {});

        Scanner scanner = scannerWithInput("-1\n");
        MenuHandler handler = new MenuHandler("Test Menu", scanner, List.of(testCommand));

        handler.handleInput();

        String output = outputStream.toString();
        assertThat(output).contains("Invalid option");
    }
}

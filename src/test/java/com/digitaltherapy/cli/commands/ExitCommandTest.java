package com.digitaltherapy.cli.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ExitCommandTest {

    @Test
    @DisplayName("execute calls the onExit callback")
    void execute_CallsOnExit() {
        AtomicBoolean exitCalled = new AtomicBoolean(false);
        ExitCommand command = new ExitCommand(() -> exitCalled.set(true));

        command.execute();

        assertThat(exitCalled.get()).isTrue();
    }

    @Test
    @DisplayName("execute prints farewell message")
    void execute_PrintsFarewellMessage() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        try {
            ExitCommand command = new ExitCommand(() -> {});
            command.execute();

            String output = outputStream.toString();
            assertThat(output).contains("Thank you for using Digital Therapy Assistant");
            assertThat(output).contains("Take care!");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("getName returns Exit")
    void getName_ReturnsExit() {
        ExitCommand command = new ExitCommand(() -> {});

        assertThat(command.getName()).isEqualTo("Exit");
    }

    @Test
    @DisplayName("getDescription returns a non-blank description")
    void getDescription_ReturnsDescription() {
        ExitCommand command = new ExitCommand(() -> {});

        assertThat(command.getDescription()).isNotBlank();
        assertThat(command.getDescription()).isEqualTo("Exit the application");
    }
}

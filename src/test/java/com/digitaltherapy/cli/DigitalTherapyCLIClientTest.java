package com.digitaltherapy.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DigitalTherapyCLIClientTest {

    @Mock
    private ApiClient apiClient;

    private PrintStream originalOut;
    private InputStream originalIn;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalIn = System.in;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    @Test
    @DisplayName("run displays welcome banner and exits on option 7")
    void run_ExitOption_DisplaysBannerAndExits() {
        // Option 7 is Exit
        System.setIn(new ByteArrayInputStream("7\n".getBytes()));

        DigitalTherapyCLIClient client = new DigitalTherapyCLIClient(apiClient);
        client.run();

        String output = outputStream.toString();
        assertThat(output).contains("Digital Therapy Assistant");
        assertThat(output).contains("Your CBT Companion");
        assertThat(output).contains("Cognitive Behavioral Therapy tools");
        assertThat(output).contains("Main Menu");
    }

    @Test
    @DisplayName("run shows all 7 menu options")
    void run_DisplaysAllMenuOptions() {
        System.setIn(new ByteArrayInputStream("7\n".getBytes()));

        DigitalTherapyCLIClient client = new DigitalTherapyCLIClient(apiClient);
        client.run();

        String output = outputStream.toString();
        // Menu should display numbered options
        assertThat(output).contains("1.");
        assertThat(output).contains("2.");
        assertThat(output).contains("3.");
        assertThat(output).contains("4.");
        assertThat(output).contains("5.");
        assertThat(output).contains("6.");
        assertThat(output).contains("7.");
    }

    @Test
    @DisplayName("run handles invalid input gracefully then exits")
    void run_InvalidThenExit_HandlesGracefully() {
        // First input is invalid, second is exit
        System.setIn(new ByteArrayInputStream("abc\n7\n".getBytes()));

        DigitalTherapyCLIClient client = new DigitalTherapyCLIClient(apiClient);
        client.run();

        String output = outputStream.toString();
        assertThat(output).contains("Please enter a valid number");
        assertThat(output).contains("Main Menu");
    }

    @Test
    @DisplayName("run handles out-of-range input then exits")
    void run_OutOfRangeThenExit_HandlesGracefully() {
        System.setIn(new ByteArrayInputStream("99\n7\n".getBytes()));

        DigitalTherapyCLIClient client = new DigitalTherapyCLIClient(apiClient);
        client.run();

        String output = outputStream.toString();
        assertThat(output).contains("Invalid option");
        assertThat(output).contains("Main Menu");
    }
}

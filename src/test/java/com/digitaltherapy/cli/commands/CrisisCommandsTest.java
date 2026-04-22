package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.ApiClient;
import com.digitaltherapy.dto.CopingStrategy;
import com.digitaltherapy.dto.CrisisHub;
import com.digitaltherapy.dto.SafetyPlanDto;
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
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrisisCommandsTest {

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
    // CrisisMenuCommand Tests
    // =========================================================================

    @Test
    @DisplayName("CrisisMenuCommand when not logged in prints login prompt")
    void crisisMenuCommand_NotLoggedIn_PrintsLoginPrompt() {
        Scanner scanner = scannerWithInput("");
        CrisisCommands.CrisisMenuCommand command = new CrisisCommands.CrisisMenuCommand(
                scanner, apiClient, () -> null);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Please log in first to access Crisis Support.");
    }

    @Test
    @DisplayName("CrisisMenuCommand getName returns Crisis Support")
    void crisisMenuCommand_GetName_ReturnsCrisisSupport() {
        Scanner scanner = scannerWithInput("");
        CrisisCommands.CrisisMenuCommand command = new CrisisCommands.CrisisMenuCommand(
                scanner, apiClient, () -> null);

        assertThat(command.getName()).isEqualTo("Crisis Support");
    }

    @Test
    @DisplayName("CrisisMenuCommand getDescription returns non-blank description")
    void crisisMenuCommand_GetDescription_ReturnsDescription() {
        Scanner scanner = scannerWithInput("");
        CrisisCommands.CrisisMenuCommand command = new CrisisCommands.CrisisMenuCommand(
                scanner, apiClient, () -> null);

        assertThat(command.getDescription()).isNotBlank();
    }

    // =========================================================================
    // CopingStrategiesCommand Tests
    // =========================================================================

    @Test
    @DisplayName("CopingStrategiesCommand success displays strategies with steps")
    @SuppressWarnings("unchecked")
    void copingStrategiesCommand_Success_DisplaysStrategiesWithSteps() {
        CopingStrategy strategy1 = CopingStrategy.builder()
                .id("cs-1")
                .name("Deep Breathing")
                .category("Relaxation")
                .description("A calming breathing technique")
                .steps(List.of("Inhale for 4 seconds", "Hold for 4 seconds", "Exhale for 4 seconds"))
                .estimatedMinutes(5)
                .build();

        CopingStrategy strategy2 = CopingStrategy.builder()
                .id("cs-2")
                .name("Grounding Exercise")
                .category("Mindfulness")
                .description("The 5-4-3-2-1 grounding technique")
                .steps(List.of("Name 5 things you see", "Name 4 things you feel"))
                .estimatedMinutes(10)
                .build();

        when(apiClient.getPublic(eq("/crisis/coping-strategies"), any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(strategy1, strategy2));

        CrisisCommands.CopingStrategiesCommand command = new CrisisCommands.CopingStrategiesCommand(apiClient);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Coping Strategies");
        assertThat(output).contains("Deep Breathing");
        assertThat(output).contains("[Relaxation]");
        assertThat(output).contains("A calming breathing technique");
        assertThat(output).contains("5 minutes");
        assertThat(output).contains("1. Inhale for 4 seconds");
        assertThat(output).contains("2. Hold for 4 seconds");
        assertThat(output).contains("3. Exhale for 4 seconds");
        assertThat(output).contains("Grounding Exercise");
        assertThat(output).contains("[Mindfulness]");
        assertThat(output).contains("The 5-4-3-2-1 grounding technique");
        assertThat(output).contains("10 minutes");
        assertThat(output).contains("1. Name 5 things you see");
        assertThat(output).contains("2. Name 4 things you feel");
    }

    @Test
    @DisplayName("CopingStrategiesCommand empty list displays no strategies message")
    @SuppressWarnings("unchecked")
    void copingStrategiesCommand_EmptyList_DisplaysNoStrategiesMessage() {
        when(apiClient.getPublic(eq("/crisis/coping-strategies"), any(ParameterizedTypeReference.class)))
                .thenReturn(Collections.emptyList());

        CrisisCommands.CopingStrategiesCommand command = new CrisisCommands.CopingStrategiesCommand(apiClient);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("No coping strategies available at this time.");
    }

    @Test
    @DisplayName("CopingStrategiesCommand API error displays failure message")
    @SuppressWarnings("unchecked")
    void copingStrategiesCommand_ApiError_DisplaysFailureMessage() {
        when(apiClient.getPublic(eq("/crisis/coping-strategies"), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        CrisisCommands.CopingStrategiesCommand command = new CrisisCommands.CopingStrategiesCommand(apiClient);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Failed to load coping strategies");
        assertThat(output).contains("Connection refused");
    }

    @Test
    @DisplayName("CopingStrategiesCommand getName returns Coping Strategies")
    void copingStrategiesCommand_GetName_ReturnsCopingStrategies() {
        CrisisCommands.CopingStrategiesCommand command = new CrisisCommands.CopingStrategiesCommand(apiClient);

        assertThat(command.getName()).isEqualTo("Coping Strategies");
    }

    @Test
    @DisplayName("CopingStrategiesCommand getDescription returns non-blank description")
    void copingStrategiesCommand_GetDescription_ReturnsDescription() {
        CrisisCommands.CopingStrategiesCommand command = new CrisisCommands.CopingStrategiesCommand(apiClient);

        assertThat(command.getDescription()).isNotBlank();
    }

    // =========================================================================
    // EmergencyResourcesCommand Tests
    // =========================================================================

    @Test
    @DisplayName("EmergencyResourcesCommand success displays resources coping and safety summary")
    void emergencyResourcesCommand_Success_DisplaysResourcesCopingAndSafety() {
        UUID userId = UUID.randomUUID();

        CrisisHub.EmergencyResource resource1 = CrisisHub.EmergencyResource.builder()
                .name("National Crisis Hotline")
                .phone("988")
                .description("24/7 crisis support line")
                .available24x7(true)
                .build();

        CrisisHub.EmergencyResource resource2 = CrisisHub.EmergencyResource.builder()
                .name("Local Crisis Center")
                .phone("555-0100")
                .description("Regional mental health support")
                .available24x7(false)
                .build();

        CrisisHub hub = CrisisHub.builder()
                .message("You are not alone. Help is available.")
                .emergencyResources(List.of(resource1, resource2))
                .copingStrategies(List.of("Deep breathing", "Call a friend"))
                .safetyPlanSummary("Review your personal safety plan regularly.")
                .build();

        when(apiClient.getPublic("/crisis", CrisisHub.class)).thenReturn(hub);

        CrisisCommands.EmergencyResourcesCommand command = new CrisisCommands.EmergencyResourcesCommand(
                apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("EMERGENCY RESOURCES");
        assertThat(output).contains("You are not alone. Help is available.");
        assertThat(output).contains("National Crisis Hotline");
        assertThat(output).contains("988");
        assertThat(output).contains("24/7 crisis support line");
        assertThat(output).contains("Available 24/7: Yes");
        assertThat(output).contains("Local Crisis Center");
        assertThat(output).contains("555-0100");
        assertThat(output).contains("Regional mental health support");
        assertThat(output).contains("Available 24/7: No");
        assertThat(output).contains("Quick Coping Strategies:");
        assertThat(output).contains("Deep breathing");
        assertThat(output).contains("Call a friend");
        assertThat(output).contains("Safety Plan Summary:");
        assertThat(output).contains("Review your personal safety plan regularly.");
    }

    @Test
    @DisplayName("EmergencyResourcesCommand null hub displays unable to load message")
    void emergencyResourcesCommand_NullHub_DisplaysUnableToLoadMessage() {
        UUID userId = UUID.randomUUID();

        when(apiClient.getPublic("/crisis", CrisisHub.class)).thenReturn(null);

        CrisisCommands.EmergencyResourcesCommand command = new CrisisCommands.EmergencyResourcesCommand(
                apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Unable to load crisis resources.");
    }

    @Test
    @DisplayName("EmergencyResourcesCommand API error displays failure message")
    void emergencyResourcesCommand_ApiError_DisplaysFailureMessage() {
        UUID userId = UUID.randomUUID();

        when(apiClient.getPublic("/crisis", CrisisHub.class))
                .thenThrow(new RuntimeException("Server unavailable"));

        CrisisCommands.EmergencyResourcesCommand command = new CrisisCommands.EmergencyResourcesCommand(
                apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Failed to load emergency resources");
        assertThat(output).contains("Server unavailable");
    }

    @Test
    @DisplayName("EmergencyResourcesCommand getName returns Emergency Resources")
    void emergencyResourcesCommand_GetName_ReturnsEmergencyResources() {
        CrisisCommands.EmergencyResourcesCommand command = new CrisisCommands.EmergencyResourcesCommand(
                apiClient, () -> UUID.randomUUID());

        assertThat(command.getName()).isEqualTo("Emergency Resources");
    }

    @Test
    @DisplayName("EmergencyResourcesCommand getDescription returns non-blank description")
    void emergencyResourcesCommand_GetDescription_ReturnsDescription() {
        CrisisCommands.EmergencyResourcesCommand command = new CrisisCommands.EmergencyResourcesCommand(
                apiClient, () -> UUID.randomUUID());

        assertThat(command.getDescription()).isNotBlank();
    }

    // =========================================================================
    // SafetyPlanCommand Tests
    // =========================================================================

    @Test
    @DisplayName("SafetyPlanCommand success displays all six sections")
    void safetyPlanCommand_Success_DisplaysAllSixSections() {
        UUID userId = UUID.randomUUID();

        SafetyPlanDto.TrustedContactDto contact1 = SafetyPlanDto.TrustedContactDto.builder()
                .name("Alice")
                .relationship("Sister")
                .phone("555-1234")
                .build();

        SafetyPlanDto.TrustedContactDto contact2 = SafetyPlanDto.TrustedContactDto.builder()
                .name("Bob")
                .relationship("Friend")
                .phone("555-5678")
                .build();

        SafetyPlanDto plan = SafetyPlanDto.builder()
                .userId(userId)
                .warningSignals(List.of("Feeling overwhelmed", "Withdrawing from others"))
                .copingStrategies(List.of("Go for a walk", "Listen to music"))
                .trustedContacts(List.of(contact1, contact2))
                .professionalContacts(List.of("Dr. Smith - 555-0001", "Therapist Jane - 555-0002"))
                .environmentSafetySteps(List.of("Remove sharp objects", "Lock medicine cabinet"))
                .reasonForLiving("My family and future goals")
                .build();

        when(apiClient.getPublic("/crisis/safety-plan", SafetyPlanDto.class)).thenReturn(plan);

        CrisisCommands.SafetyPlanCommand command = new CrisisCommands.SafetyPlanCommand(
                apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("My Safety Plan");

        // Section 1: Warning Signals
        assertThat(output).contains("1. Warning Signals:");
        assertThat(output).contains("Feeling overwhelmed");
        assertThat(output).contains("Withdrawing from others");

        // Section 2: Coping Strategies
        assertThat(output).contains("2. Coping Strategies:");
        assertThat(output).contains("Go for a walk");
        assertThat(output).contains("Listen to music");

        // Section 3: Trusted Contacts
        assertThat(output).contains("3. Trusted Contacts:");
        assertThat(output).contains("Alice (Sister): 555-1234");
        assertThat(output).contains("Bob (Friend): 555-5678");

        // Section 4: Professional Contacts
        assertThat(output).contains("4. Professional Contacts:");
        assertThat(output).contains("Dr. Smith - 555-0001");
        assertThat(output).contains("Therapist Jane - 555-0002");

        // Section 5: Environment Safety Steps
        assertThat(output).contains("5. Environment Safety Steps:");
        assertThat(output).contains("Remove sharp objects");
        assertThat(output).contains("Lock medicine cabinet");

        // Section 6: Reason for Living
        assertThat(output).contains("6. Reason for Living:");
        assertThat(output).contains("My family and future goals");
    }

    @Test
    @DisplayName("SafetyPlanCommand null plan displays no plan found message")
    void safetyPlanCommand_NullPlan_DisplaysNoPlanFoundMessage() {
        UUID userId = UUID.randomUUID();

        when(apiClient.getPublic("/crisis/safety-plan", SafetyPlanDto.class)).thenReturn(null);

        CrisisCommands.SafetyPlanCommand command = new CrisisCommands.SafetyPlanCommand(
                apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("No safety plan found. Consider creating one with your therapist.");
    }

    @Test
    @DisplayName("SafetyPlanCommand API error displays failure message")
    void safetyPlanCommand_ApiError_DisplaysFailureMessage() {
        UUID userId = UUID.randomUUID();

        when(apiClient.getPublic("/crisis/safety-plan", SafetyPlanDto.class))
                .thenThrow(new RuntimeException("Timeout"));

        CrisisCommands.SafetyPlanCommand command = new CrisisCommands.SafetyPlanCommand(
                apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Failed to load safety plan");
        assertThat(output).contains("Timeout");
    }

    @Test
    @DisplayName("SafetyPlanCommand getName returns My Safety Plan")
    void safetyPlanCommand_GetName_ReturnsMySafetyPlan() {
        CrisisCommands.SafetyPlanCommand command = new CrisisCommands.SafetyPlanCommand(
                apiClient, () -> UUID.randomUUID());

        assertThat(command.getName()).isEqualTo("My Safety Plan");
    }

    @Test
    @DisplayName("SafetyPlanCommand getDescription returns non-blank description")
    void safetyPlanCommand_GetDescription_ReturnsDescription() {
        CrisisCommands.SafetyPlanCommand command = new CrisisCommands.SafetyPlanCommand(
                apiClient, () -> UUID.randomUUID());

        assertThat(command.getDescription()).isNotBlank();
    }

    // =========================================================================
    // CrisisMenuCommand — Logged In Path
    // =========================================================================

    @Test
    @DisplayName("CrisisMenuCommand when logged in displays sub-menu and selects Back")
    void crisisMenuCommand_LoggedIn_DisplaysSubMenu() {
        UUID userId = UUID.randomUUID();
        // Input: "4" selects Back (CopingStrategies, EmergencyResources, SafetyPlan, Back)
        Scanner scanner = scannerWithInput("4\n");
        CrisisCommands.CrisisMenuCommand command = new CrisisCommands.CrisisMenuCommand(
                scanner, apiClient, () -> userId);

        command.execute();

        String output = outputStream.toString();
        assertThat(output).contains("Crisis Support");
        assertThat(output).contains("Back to Main Menu");
    }

}

package com.digitaltherapy.controller;

import com.digitaltherapy.config.JwtTokenProvider;
import com.digitaltherapy.dto.*;
import com.digitaltherapy.repository.UserRepository;
import com.digitaltherapy.service.CrisisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CrisisController.class)
@AutoConfigureMockMvc(addFilters = false)
class CrisisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrisisService crisisService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private ObjectMapper objectMapper;

    private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ----------------------------------------------------------- getCrisisHub
    @Test
    void getCrisisHub_Returns200() throws Exception {
        CrisisHub hub = CrisisHub.builder()
                .message("You are not alone. Help is available.")
                .emergencyResources(List.of(
                        CrisisHub.EmergencyResource.builder()
                                .name("988 Suicide & Crisis Lifeline")
                                .phone("988")
                                .description("Free, confidential support for people in distress.")
                                .available24x7(true)
                                .build(),
                        CrisisHub.EmergencyResource.builder()
                                .name("Emergency Services")
                                .phone("911")
                                .description("For immediate life-threatening emergencies.")
                                .available24x7(true)
                                .build()
                ))
                .copingStrategies(List.of(
                        "Practice deep breathing",
                        "Use the 5-4-3-2-1 grounding technique"
                ))
                .safetyPlanSummary("You have 2 trusted contact(s) in your safety plan.")
                .build();

        when(crisisService.getCrisisHub(any())).thenReturn(hub);

        mockMvc.perform(get("/crisis")
                        .param("userId", TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("You are not alone. Help is available."))
                .andExpect(jsonPath("$.emergencyResources[0].name").value("988 Suicide & Crisis Lifeline"))
                .andExpect(jsonPath("$.emergencyResources[0].phone").value("988"))
                .andExpect(jsonPath("$.emergencyResources[0].available24x7").value(true))
                .andExpect(jsonPath("$.emergencyResources[1].name").value("Emergency Services"))
                .andExpect(jsonPath("$.emergencyResources[1].phone").value("911"))
                .andExpect(jsonPath("$.copingStrategies[0]").value("Practice deep breathing"))
                .andExpect(jsonPath("$.copingStrategies[1]").value("Use the 5-4-3-2-1 grounding technique"))
                .andExpect(jsonPath("$.safetyPlanSummary").value("You have 2 trusted contact(s) in your safety plan."));
    }

    // ----------------------------------------------------- getCopingStrategies
    @Test
    void getCopingStrategies_Returns200() throws Exception {
        CopingStrategy strategy = CopingStrategy.builder()
                .id("breathing")
                .name("Deep Breathing Exercise")
                .description("A calming breathing technique to reduce anxiety and stress.")
                .category("Relaxation")
                .steps(List.of(
                        "Find a comfortable seated position.",
                        "Breathe in slowly through your nose for 4 seconds.",
                        "Hold your breath for 4 seconds.",
                        "Exhale slowly through your mouth for 4 seconds."
                ))
                .estimatedMinutes(5)
                .build();

        when(crisisService.getCopingStrategies()).thenReturn(List.of(strategy));

        mockMvc.perform(get("/crisis/coping-strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("breathing"))
                .andExpect(jsonPath("$[0].name").value("Deep Breathing Exercise"))
                .andExpect(jsonPath("$[0].description").value("A calming breathing technique to reduce anxiety and stress."))
                .andExpect(jsonPath("$[0].category").value("Relaxation"))
                .andExpect(jsonPath("$[0].steps[0]").value("Find a comfortable seated position."))
                .andExpect(jsonPath("$[0].steps[1]").value("Breathe in slowly through your nose for 4 seconds."))
                .andExpect(jsonPath("$[0].estimatedMinutes").value(5));
    }

    // ----------------------------------------------------------- detectCrisis
    @Test
    void detectCrisis_ValidRequest_Returns200() throws Exception {
        CrisisDetectRequest request = CrisisDetectRequest.builder()
                .text("I feel very overwhelmed with everything in my life right now")
                .build();

        CrisisDetectionResultDto result = CrisisDetectionResultDto.builder()
                .riskLevel("LOW")
                .keywordsDetected(List.of("overwhelmed"))
                .recommendedAction("MONITOR")
                .reasoning("The text indicates distress but no immediate danger indicators are present.")
                .build();

        when(crisisService.detectCrisis(any(String.class))).thenReturn(result);

        mockMvc.perform(post("/crisis/detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.keywordsDetected[0]").value("overwhelmed"))
                .andExpect(jsonPath("$.recommendedAction").value("MONITOR"))
                .andExpect(jsonPath("$.reasoning").value("The text indicates distress but no immediate danger indicators are present."));
    }

    @Test
    void detectCrisis_BlankText_Returns400() throws Exception {
        CrisisDetectRequest request = CrisisDetectRequest.builder()
                .text("")
                .build();

        mockMvc.perform(post("/crisis/detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------- getSafetyPlan
    @Test
    void getSafetyPlan_Returns200() throws Exception {
        SafetyPlanDto plan = SafetyPlanDto.builder()
                .userId(TEST_USER_ID)
                .warningSignals(List.of(
                        "Feeling overwhelmed or hopeless",
                        "Withdrawing from friends and activities"
                ))
                .copingStrategies(List.of(
                        "Practice deep breathing exercises",
                        "Use the 5-4-3-2-1 grounding technique"
                ))
                .trustedContacts(List.of(
                        SafetyPlanDto.TrustedContactDto.builder()
                                .name("Jane Doe")
                                .phone("555-1234")
                                .relationship("Sister")
                                .build()
                ))
                .professionalContacts(List.of(
                        "988 Suicide & Crisis Lifeline: 988",
                        "Crisis Text Line: Text HOME to 741741"
                ))
                .environmentSafetySteps(List.of(
                        "Remove or secure any items that could be used for self-harm",
                        "Stay in a safe location"
                ))
                .reasonForLiving("My family and future goals")
                .build();

        when(crisisService.getSafetyPlan(any(UUID.class))).thenReturn(plan);

        mockMvc.perform(get("/crisis/safety-plan")
                        .param("userId", TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.warningSignals[0]").value("Feeling overwhelmed or hopeless"))
                .andExpect(jsonPath("$.warningSignals[1]").value("Withdrawing from friends and activities"))
                .andExpect(jsonPath("$.copingStrategies[0]").value("Practice deep breathing exercises"))
                .andExpect(jsonPath("$.trustedContacts[0].name").value("Jane Doe"))
                .andExpect(jsonPath("$.trustedContacts[0].phone").value("555-1234"))
                .andExpect(jsonPath("$.trustedContacts[0].relationship").value("Sister"))
                .andExpect(jsonPath("$.professionalContacts[0]").value("988 Suicide & Crisis Lifeline: 988"))
                .andExpect(jsonPath("$.environmentSafetySteps[0]").value("Remove or secure any items that could be used for self-harm"))
                .andExpect(jsonPath("$.reasonForLiving").value("My family and future goals"));
    }

    // ------------------------------------------------------- updateSafetyPlan
    @Test
    void updateSafetyPlan_Returns200() throws Exception {
        SafetyPlanUpdate updateRequest = SafetyPlanUpdate.builder()
                .warningSignals(List.of("Feeling hopeless", "Not sleeping"))
                .copingStrategies(List.of("Deep breathing", "Go for a walk"))
                .trustedContacts(List.of(
                        SafetyPlanDto.TrustedContactDto.builder()
                                .name("John Smith")
                                .phone("555-5678")
                                .relationship("Best friend")
                                .build()
                ))
                .professionalContacts(List.of("988 Suicide & Crisis Lifeline: 988"))
                .environmentSafetySteps(List.of("Stay in a safe location"))
                .reasonForLiving("My children and hobbies")
                .build();

        SafetyPlanDto updatedPlan = SafetyPlanDto.builder()
                .userId(TEST_USER_ID)
                .warningSignals(List.of("Feeling hopeless", "Not sleeping"))
                .copingStrategies(List.of("Deep breathing", "Go for a walk"))
                .trustedContacts(List.of(
                        SafetyPlanDto.TrustedContactDto.builder()
                                .name("John Smith")
                                .phone("555-5678")
                                .relationship("Best friend")
                                .build()
                ))
                .professionalContacts(List.of("988 Suicide & Crisis Lifeline: 988"))
                .environmentSafetySteps(List.of("Stay in a safe location"))
                .reasonForLiving("My children and hobbies")
                .build();

        when(crisisService.updateSafetyPlan(any(UUID.class), any(SafetyPlanUpdate.class)))
                .thenReturn(updatedPlan);

        mockMvc.perform(put("/crisis/safety-plan")
                        .param("userId", TEST_USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.warningSignals[0]").value("Feeling hopeless"))
                .andExpect(jsonPath("$.warningSignals[1]").value("Not sleeping"))
                .andExpect(jsonPath("$.copingStrategies[0]").value("Deep breathing"))
                .andExpect(jsonPath("$.copingStrategies[1]").value("Go for a walk"))
                .andExpect(jsonPath("$.trustedContacts[0].name").value("John Smith"))
                .andExpect(jsonPath("$.trustedContacts[0].phone").value("555-5678"))
                .andExpect(jsonPath("$.trustedContacts[0].relationship").value("Best friend"))
                .andExpect(jsonPath("$.professionalContacts[0]").value("988 Suicide & Crisis Lifeline: 988"))
                .andExpect(jsonPath("$.environmentSafetySteps[0]").value("Stay in a safe location"))
                .andExpect(jsonPath("$.reasonForLiving").value("My children and hobbies"));
    }
}

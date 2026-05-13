package com.digitaltherapy.service.impl;

import com.digitaltherapy.dto.*;
import com.digitaltherapy.entity.TrustedContact;
import com.digitaltherapy.entity.User;
import com.digitaltherapy.exception.ResourceNotFoundException;
import com.digitaltherapy.repository.TrustedContactRepository;
import com.digitaltherapy.repository.UserRepository;
import com.digitaltherapy.service.rag.CrisisDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrisisServiceImplTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private TrustedContactRepository trustedContactRepository;

    @Mock
    private CrisisDetector crisisDetector;

    @InjectMocks
    private CrisisServiceImpl crisisService;

    private User testUser;
    private TrustedContact testContact;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .email("test@example.com")
                .passwordHash("encoded_password")
                .name("Test User")
                .streakDays(5)
                .build();

        testContact = TrustedContact.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("Jane Doe")
                .phone("555-1234")
                .relationship("Sister")
                .build();
    }

    @Test
    @DisplayName("getCrisisHub - returns hub with emergency resources and safety plan summary")
    void getCrisisHub_ReturnsHubWithResources() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(trustedContactRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of(testContact));

        // Act
        CrisisHub result = crisisService.getCrisisHub(TEST_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMessage()).contains("You are not alone");
        assertThat(result.getEmergencyResources()).hasSize(5);

        // Verify emergency resources contain known entries
        assertThat(result.getEmergencyResources())
                .extracting(CrisisHub.EmergencyResource::getName)
                .contains("988 Suicide & Crisis Lifeline", "Crisis Text Line", "Emergency Services");

        // All resources should be 24/7
        assertThat(result.getEmergencyResources())
                .allMatch(CrisisHub.EmergencyResource::isAvailable24x7);

        assertThat(result.getCopingStrategies()).hasSize(5);
        assertThat(result.getCopingStrategies().get(0)).contains("deep breathing");

        // With 1 contact, safety plan should mention the count
        assertThat(result.getSafetyPlanSummary()).contains("1 trusted contact(s)");

        verify(userRepository).findById(TEST_USER_ID);
        verify(trustedContactRepository).findByUserId(TEST_USER_ID);
    }

    @Test
    @DisplayName("getCrisisHub - user not found - throws ResourceNotFoundException")
    void getCrisisHub_UserNotFound_ThrowsException() {
        // Arrange
        UUID missingUserId = UUID.randomUUID();
        when(userRepository.findById(missingUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> crisisService.getCrisisHub(missingUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(missingUserId);
    }

    @Test
    @DisplayName("getCopingStrategies - returns list of coping strategies with details")
    void getCopingStrategies_ReturnsStrategies() {
        // Act
        List<CopingStrategy> result = crisisService.getCopingStrategies();

        // Assert
        assertThat(result).hasSize(6);

        CopingStrategy breathing = result.stream()
                .filter(s -> "breathing".equals(s.getId())).findFirst().orElseThrow();
        assertThat(breathing.getName()).isEqualTo("Deep Breathing Exercise");
        assertThat(breathing.getCategory()).isEqualTo("Relaxation");
        assertThat(breathing.getSteps()).isNotEmpty();
        assertThat(breathing.getEstimatedMinutes()).isEqualTo(5);

        CopingStrategy grounding = result.stream()
                .filter(s -> "grounding_54321".equals(s.getId())).findFirst().orElseThrow();
        assertThat(grounding.getName()).isEqualTo("5-4-3-2-1 Grounding");
        assertThat(grounding.getCategory()).isEqualTo("Grounding");
        assertThat(grounding.getEstimatedMinutes()).isEqualTo(3);

        CopingStrategy thoughtChallenging = result.stream()
                .filter(s -> "thought_challenging".equals(s.getId())).findFirst().orElseThrow();
        assertThat(thoughtChallenging.getCategory()).isEqualTo("Cognitive");

        // Verify all strategies have required fields
        for (CopingStrategy strategy : result) {
            assertThat(strategy.getId()).isNotBlank();
            assertThat(strategy.getName()).isNotBlank();
            assertThat(strategy.getDescription()).isNotBlank();
            assertThat(strategy.getCategory()).isNotBlank();
            assertThat(strategy.getSteps()).isNotEmpty();
            assertThat(strategy.getEstimatedMinutes()).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("detectCrisis - delegates to CrisisDetector and returns result")
    void detectCrisis_DelegatesToDetector() {
        // Arrange
        String text = "I feel very sad and hopeless about my situation";
        CrisisDetectionResultDto detectorResult = CrisisDetectionResultDto.builder()
                .riskLevel("medium")
                .keywordsDetected(List.of())
                .recommendedAction("show_resources")
                .reasoning("AI assessment: MEDIUM. Final risk level: MEDIUM.")
                .build();

        when(crisisDetector.analyze(text)).thenReturn(detectorResult);

        // Act
        CrisisDetectionResultDto result = crisisService.detectCrisis(text);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRiskLevel()).isEqualTo("medium");
        assertThat(result.getRecommendedAction()).isEqualTo("show_resources");
        assertThat(result.getReasoning()).contains("MEDIUM");
        assertThat(result.getKeywordsDetected()).isEmpty();

        verify(crisisDetector).analyze(text);
    }

    @Test
    @DisplayName("detectCrisis - detector failure - returns safe default")
    void detectCrisis_DetectorFailure_ReturnsSafeDefault() {
        // Arrange
        String text = "Some text input";
        when(crisisDetector.analyze(text)).thenThrow(new RuntimeException("Detector service unavailable"));

        // Act
        CrisisDetectionResultDto result = crisisService.detectCrisis(text);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRiskLevel()).isEqualTo("NONE");
        assertThat(result.getKeywordsDetected()).isEmpty();
        assertThat(result.getRecommendedAction()).isEqualTo("NONE");
        assertThat(result.getReasoning()).contains("temporarily unavailable");
        assertThat(result.getReasoning()).contains("988");

        verify(crisisDetector).analyze(text);
    }

    @Test
    @DisplayName("getSafetyPlan - returns safety plan with user trusted contacts")
    void getSafetyPlan_ReturnsUserPlan() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(trustedContactRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of(testContact));

        // Act
        SafetyPlanDto result = crisisService.getSafetyPlan(TEST_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);

        // Default warning signals
        assertThat(result.getWarningSignals()).hasSize(4);
        assertThat(result.getWarningSignals()).contains("Feeling overwhelmed or hopeless");

        // Default coping strategies
        assertThat(result.getCopingStrategies()).hasSize(4);
        assertThat(result.getCopingStrategies()).contains("Practice deep breathing exercises");

        // Trusted contacts from repository
        assertThat(result.getTrustedContacts()).hasSize(1);
        SafetyPlanDto.TrustedContactDto contactDto = result.getTrustedContacts().get(0);
        assertThat(contactDto.getName()).isEqualTo("Jane Doe");
        assertThat(contactDto.getPhone()).isEqualTo("555-1234");
        assertThat(contactDto.getRelationship()).isEqualTo("Sister");

        // Professional contacts
        assertThat(result.getProfessionalContacts()).hasSize(2);
        assertThat(result.getProfessionalContacts().get(0)).contains("988");

        // Environment safety steps
        assertThat(result.getEnvironmentSafetySteps()).hasSize(3);

        // Reason for living default
        assertThat(result.getReasonForLiving()).isEmpty();

        verify(userRepository).findById(TEST_USER_ID);
        verify(trustedContactRepository).findByUserId(TEST_USER_ID);
    }

    @Test
    @DisplayName("updateSafetyPlan - updates trusted contacts and returns updated plan")
    void updateSafetyPlan_UpdatesContacts() {
        // Arrange
        SafetyPlanDto.TrustedContactDto newContactDto = SafetyPlanDto.TrustedContactDto.builder()
                .name("John Smith")
                .phone("555-5678")
                .relationship("Brother")
                .build();

        SafetyPlanUpdate update = SafetyPlanUpdate.builder()
                .warningSignals(List.of("Feeling overwhelmed"))
                .copingStrategies(List.of("Deep breathing"))
                .trustedContacts(List.of(newContactDto))
                .professionalContacts(List.of("988 Lifeline"))
                .environmentSafetySteps(List.of("Stay safe"))
                .reasonForLiving("My family")
                .build();

        TrustedContact newContact = TrustedContact.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("John Smith")
                .phone("555-5678")
                .relationship("Brother")
                .build();

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        // First call: existing contacts to delete
        when(trustedContactRepository.findByUserId(TEST_USER_ID))
                .thenReturn(List.of(testContact))       // for deletion
                .thenReturn(List.of(newContact));        // for rebuilding the response

        when(trustedContactRepository.saveAll(anyList())).thenReturn(List.of(newContact));

        // Act
        SafetyPlanDto result = crisisService.updateSafetyPlan(TEST_USER_ID, update);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getWarningSignals()).containsExactly("Feeling overwhelmed");
        assertThat(result.getCopingStrategies()).containsExactly("Deep breathing");
        assertThat(result.getProfessionalContacts()).containsExactly("988 Lifeline");
        assertThat(result.getEnvironmentSafetySteps()).containsExactly("Stay safe");
        assertThat(result.getReasonForLiving()).isEqualTo("My family");

        // Updated trusted contact
        assertThat(result.getTrustedContacts()).hasSize(1);
        assertThat(result.getTrustedContacts().get(0).getName()).isEqualTo("John Smith");
        assertThat(result.getTrustedContacts().get(0).getPhone()).isEqualTo("555-5678");
        assertThat(result.getTrustedContacts().get(0).getRelationship()).isEqualTo("Brother");

        verify(userRepository).findById(TEST_USER_ID);
        verify(trustedContactRepository).deleteAll(List.of(testContact));
        verify(trustedContactRepository).saveAll(anyList());
    }
}

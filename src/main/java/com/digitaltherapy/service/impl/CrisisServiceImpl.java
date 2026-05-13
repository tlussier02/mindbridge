package com.digitaltherapy.service.impl;

import com.digitaltherapy.dto.*;
import com.digitaltherapy.entity.TrustedContact;
import com.digitaltherapy.entity.User;
import com.digitaltherapy.exception.ResourceNotFoundException;
import com.digitaltherapy.repository.TrustedContactRepository;
import com.digitaltherapy.repository.UserRepository;
import com.digitaltherapy.service.CrisisService;
import com.digitaltherapy.service.rag.CrisisDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrisisServiceImpl implements CrisisService {

    private final UserRepository userRepository;
    private final TrustedContactRepository trustedContactRepository;
    private final CrisisDetector crisisDetector;

    @Override
    public CrisisHub getCrisisHub(UUID userId) {
        log.info("Fetching crisis hub for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<CrisisHub.EmergencyResource> emergencyResources = List.of(
                CrisisHub.EmergencyResource.builder()
                        .name("988 Suicide & Crisis Lifeline")
                        .phone("988")
                        .description("Free, confidential support for people in distress. Call or text 988.")
                        .available24x7(true)
                        .build(),
                CrisisHub.EmergencyResource.builder()
                        .name("Crisis Text Line")
                        .phone("741741")
                        .description("Text HOME to 741741 to connect with a trained crisis counselor.")
                        .available24x7(true)
                        .build(),
                CrisisHub.EmergencyResource.builder()
                        .name("National Domestic Violence Hotline")
                        .phone("1-800-799-7233")
                        .description("Support for anyone affected by domestic violence.")
                        .available24x7(true)
                        .build(),
                CrisisHub.EmergencyResource.builder()
                        .name("SAMHSA National Helpline")
                        .phone("1-800-662-4357")
                        .description("Treatment referral and information service for mental health and substance use.")
                        .available24x7(true)
                        .build(),
                CrisisHub.EmergencyResource.builder()
                        .name("Emergency Services")
                        .phone("911")
                        .description("For immediate life-threatening emergencies.")
                        .available24x7(true)
                        .build()
        );

        List<String> copingStrategies = List.of(
                "Practice deep breathing: Inhale for 4 counts, hold for 4, exhale for 4.",
                "Use the 5-4-3-2-1 grounding technique: Notice 5 things you see, 4 you touch, 3 you hear, 2 you smell, 1 you taste.",
                "Reach out to a trusted friend or family member.",
                "Engage in a physical activity like walking or stretching.",
                "Write down your thoughts in your thought diary."
        );

        // Build safety plan summary from trusted contacts
        List<TrustedContact> contacts = trustedContactRepository.findByUserId(userId);
        String safetyPlanSummary;
        if (contacts.isEmpty()) {
            safetyPlanSummary = "Your safety plan has not been set up yet. Consider adding trusted contacts and coping strategies.";
        } else {
            safetyPlanSummary = "You have " + contacts.size() + " trusted contact(s) in your safety plan. "
                    + "Review your full safety plan for complete details.";
        }

        return CrisisHub.builder()
                .message("You are not alone. Help is available. If you are in immediate danger, please call 911.")
                .emergencyResources(emergencyResources)
                .copingStrategies(copingStrategies)
                .safetyPlanSummary(safetyPlanSummary)
                .build();
    }

    @Override
    @Cacheable("copingStrategies")
    public List<CopingStrategy> getCopingStrategies() {
        log.info("Fetching coping strategies");

        return List.of(
                CopingStrategy.builder()
                        .id("breathing")
                        .name("Deep Breathing Exercise")
                        .description("A calming breathing technique to reduce anxiety and stress.")
                        .category("Relaxation")
                        .steps(List.of(
                                "Find a comfortable seated position.",
                                "Close your eyes and relax your shoulders.",
                                "Breathe in slowly through your nose for 4 seconds.",
                                "Hold your breath for 4 seconds.",
                                "Exhale slowly through your mouth for 4 seconds.",
                                "Repeat for 5-10 minutes."))
                        .estimatedMinutes(5)
                        .build(),
                CopingStrategy.builder()
                        .id("grounding_54321")
                        .name("5-4-3-2-1 Grounding")
                        .description("A sensory awareness exercise to bring you back to the present moment.")
                        .category("Grounding")
                        .steps(List.of(
                                "Acknowledge 5 things you can SEE around you.",
                                "Acknowledge 4 things you can TOUCH.",
                                "Acknowledge 3 things you can HEAR.",
                                "Acknowledge 2 things you can SMELL.",
                                "Acknowledge 1 thing you can TASTE."))
                        .estimatedMinutes(3)
                        .build(),
                CopingStrategy.builder()
                        .id("progressive_muscle")
                        .name("Progressive Muscle Relaxation")
                        .description("Systematically tense and relax muscle groups to release physical tension.")
                        .category("Relaxation")
                        .steps(List.of(
                                "Start with your feet. Tense the muscles for 5 seconds.",
                                "Release and notice the difference for 10 seconds.",
                                "Move to your calves, then thighs, repeating the pattern.",
                                "Continue through abdomen, chest, arms, and face.",
                                "End by taking a few deep breaths."))
                        .estimatedMinutes(10)
                        .build(),
                CopingStrategy.builder()
                        .id("thought_challenging")
                        .name("Thought Challenging")
                        .description("Identify and challenge negative automatic thoughts using CBT techniques.")
                        .category("Cognitive")
                        .steps(List.of(
                                "Write down the negative thought.",
                                "Identify what cognitive distortion it might represent.",
                                "Ask yourself: What evidence supports this thought?",
                                "Ask yourself: What evidence contradicts this thought?",
                                "Write a balanced alternative thought."))
                        .estimatedMinutes(10)
                        .build(),
                CopingStrategy.builder()
                        .id("safe_place_visualization")
                        .name("Safe Place Visualization")
                        .description("Visualize a calming, safe environment to reduce distress.")
                        .category("Visualization")
                        .steps(List.of(
                                "Close your eyes and take several deep breaths.",
                                "Imagine a place where you feel completely safe and calm.",
                                "Notice what you see, hear, smell, and feel in this place.",
                                "Allow the feelings of safety and comfort to wash over you.",
                                "Stay in this place for as long as you need."))
                        .estimatedMinutes(5)
                        .build(),
                CopingStrategy.builder()
                        .id("physical_activity")
                        .name("Physical Activity Break")
                        .description("Use movement to release tension and improve mood through endorphins.")
                        .category("Physical")
                        .steps(List.of(
                                "Stand up and stretch your body.",
                                "Take a brisk walk around your space or outside.",
                                "Do simple exercises: jumping jacks, stretches, or yoga poses.",
                                "Focus on your body sensations and breathing during movement.",
                                "Cool down with gentle stretching."))
                        .estimatedMinutes(10)
                        .build()
        );
    }

    @Override
    public CrisisDetectionResultDto detectCrisis(String text) {
        log.info("Running crisis detection analysis");

        try {
            return crisisDetector.analyze(text);
        } catch (Exception e) {
            log.warn("Crisis detector unavailable, returning safe default", e);
            return CrisisDetectionResultDto.builder()
                    .riskLevel("NONE")
                    .keywordsDetected(List.of())
                    .recommendedAction("NONE")
                    .reasoning("Crisis detection service is temporarily unavailable. "
                            + "If you are in crisis, please call 988 or text HOME to 741741.")
                    .build();
        }
    }

    @Override
    public SafetyPlanDto getSafetyPlan(UUID userId) {
        log.info("Fetching safety plan for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<TrustedContact> contacts = trustedContactRepository.findByUserId(userId);

        List<SafetyPlanDto.TrustedContactDto> contactDtos = contacts.stream()
                .map(c -> SafetyPlanDto.TrustedContactDto.builder()
                        .name(c.getName())
                        .phone(c.getPhone())
                        .relationship(c.getRelationship())
                        .build())
                .collect(Collectors.toList());

        return SafetyPlanDto.builder()
                .userId(userId)
                .warningSignals(List.of(
                        "Feeling overwhelmed or hopeless",
                        "Withdrawing from friends and activities",
                        "Increased irritability or agitation",
                        "Changes in sleep or appetite"
                ))
                .copingStrategies(List.of(
                        "Practice deep breathing exercises",
                        "Use the 5-4-3-2-1 grounding technique",
                        "Go for a walk or engage in physical activity",
                        "Write in your thought diary"
                ))
                .trustedContacts(contactDtos)
                .professionalContacts(List.of(
                        "988 Suicide & Crisis Lifeline: 988",
                        "Crisis Text Line: Text HOME to 741741"
                ))
                .environmentSafetySteps(List.of(
                        "Remove or secure any items that could be used for self-harm",
                        "Stay in a safe location",
                        "Reach out to someone you trust"
                ))
                .reasonForLiving("")
                .build();
    }

    @Override
    @Transactional
    public SafetyPlanDto updateSafetyPlan(UUID userId, SafetyPlanUpdate update) {
        log.info("Updating safety plan for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Update trusted contacts
        if (update.getTrustedContacts() != null) {
            // Remove existing contacts
            List<TrustedContact> existing = trustedContactRepository.findByUserId(userId);
            trustedContactRepository.deleteAll(existing);

            // Save new contacts
            List<TrustedContact> newContacts = update.getTrustedContacts().stream()
                    .map(dto -> TrustedContact.builder()
                            .user(user)
                            .name(dto.getName())
                            .phone(dto.getPhone())
                            .relationship(dto.getRelationship())
                            .build())
                    .collect(Collectors.toList());
            trustedContactRepository.saveAll(newContacts);
        }

        // Rebuild and return the updated safety plan
        List<TrustedContact> updatedContacts = trustedContactRepository.findByUserId(userId);
        List<SafetyPlanDto.TrustedContactDto> contactDtos = updatedContacts.stream()
                .map(c -> SafetyPlanDto.TrustedContactDto.builder()
                        .name(c.getName())
                        .phone(c.getPhone())
                        .relationship(c.getRelationship())
                        .build())
                .collect(Collectors.toList());

        return SafetyPlanDto.builder()
                .userId(userId)
                .warningSignals(update.getWarningSignals() != null
                        ? update.getWarningSignals()
                        : List.of())
                .copingStrategies(update.getCopingStrategies() != null
                        ? update.getCopingStrategies()
                        : List.of())
                .trustedContacts(contactDtos)
                .professionalContacts(update.getProfessionalContacts() != null
                        ? update.getProfessionalContacts()
                        : List.of())
                .environmentSafetySteps(update.getEnvironmentSafetySteps() != null
                        ? update.getEnvironmentSafetySteps()
                        : List.of())
                .reasonForLiving(update.getReasonForLiving() != null
                        ? update.getReasonForLiving()
                        : "")
                .build();
    }
}

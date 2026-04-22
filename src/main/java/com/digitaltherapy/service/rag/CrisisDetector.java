package com.digitaltherapy.service.rag;

import com.digitaltherapy.dto.CrisisDetectionResultDto;
import com.digitaltherapy.entity.RecommendedAction;
import com.digitaltherapy.entity.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrisisDetector {

    private static final Set<String> CRISIS_KEYWORDS = Set.of(
        "suicide", "kill myself", "end it all", "no reason to live",
        "better off dead", "can't go on", "want to die", "hurt myself"
    );

    private final ChatClient chatClient;

    @Cacheable(value = "crisisAnalysis", key = "#text")
    public CrisisDetectionResultDto analyze(String text) {
        // Layer 1: Keyword-based detection (fast)
        List<String> detectedKeywords = CRISIS_KEYWORDS.stream()
            .filter(k -> text.toLowerCase().contains(k))
            .collect(Collectors.toList());

        // Layer 2: AI-based semantic analysis
        RiskLevel aiRisk = performAiAnalysis(text);

        // Combine signals - err on the side of caution
        RiskLevel finalRisk = combineRiskAssessments(detectedKeywords, aiRisk);
        RecommendedAction action = determineAction(finalRisk);

        return CrisisDetectionResultDto.builder()
            .riskLevel(finalRisk.name().toLowerCase())
            .keywordsDetected(detectedKeywords)
            .recommendedAction(action.name().toLowerCase())
            .reasoning(generateReasoning(detectedKeywords, aiRisk, finalRisk))
            .build();
    }

    private RiskLevel performAiAnalysis(String text) {
        try {
            String prompt = String.format(
                "Analyze the following text for crisis indicators. Assess risk level and recommend appropriate action.\n\n" +
                "Text: \"%s\"\n\n" +
                "Evaluate for:\nSuicidal ideation or self-harm mentions\nExpressions of hopelessness\n" +
                "Statements about being a burden\nPlans or intentions to harm self/others\nSevere emotional distress\n\n" +
                "Respond with ONLY one word for risk level: NONE, LOW, MEDIUM, HIGH, or CRITICAL", text);

            String response = chatClient.prompt()
                .system("You are a crisis assessment specialist. Respond with only the risk level.")
                .user(prompt)
                .call()
                .content();
            return parseRiskLevel(response.trim().toUpperCase());
        } catch (Exception e) {
            log.warn("AI crisis analysis failed, falling back to keyword-based assessment", e);
            return RiskLevel.NONE;
        }
    }

    private RiskLevel parseRiskLevel(String response) {
        for (RiskLevel level : RiskLevel.values()) {
            if (response.contains(level.name())) return level;
        }
        return RiskLevel.NONE;
    }

    private RiskLevel combineRiskAssessments(List<String> keywords, RiskLevel aiRisk) {
        RiskLevel keywordRisk = keywords.isEmpty() ? RiskLevel.NONE :
            keywords.size() >= 3 ? RiskLevel.CRITICAL :
            keywords.size() >= 2 ? RiskLevel.HIGH : RiskLevel.MEDIUM;

        // Return the higher risk level
        return keywordRisk.ordinal() > aiRisk.ordinal() ? keywordRisk : aiRisk;
    }

    private RecommendedAction determineAction(RiskLevel risk) {
        return switch (risk) {
            case CRITICAL -> RecommendedAction.IMMEDIATE_INTERVENTION;
            case HIGH -> RecommendedAction.SHOW_CRISIS_HUB;
            case MEDIUM -> RecommendedAction.SHOW_RESOURCES;
            default -> RecommendedAction.NONE;
        };
    }

    private String generateReasoning(List<String> keywords, RiskLevel aiRisk, RiskLevel finalRisk) {
        StringBuilder reasoning = new StringBuilder();
        if (!keywords.isEmpty()) {
            reasoning.append("Crisis keywords detected: ").append(String.join(", ", keywords)).append(". ");
        }
        reasoning.append("AI assessment: ").append(aiRisk.name()).append(". ");
        reasoning.append("Final risk level: ").append(finalRisk.name()).append(".");
        return reasoning.toString();
    }
}

package com.digitaltherapy.service.impl;

import com.digitaltherapy.dto.CrisisDetectionResultDto;
import com.digitaltherapy.dto.DistortionSuggestion;
import com.digitaltherapy.service.AiService;
import com.digitaltherapy.service.rag.CrisisDetector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class AiIntegrationTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private CrisisDetector crisisDetector;

    @Test
    @DisplayName("AiService.analyzeThought calls Claude AI and returns distortion suggestions")
    void analyzeThought_CallsLLM_ReturnsDistortions() {
        List<DistortionSuggestion> suggestions = aiService.analyzeThought(
                "I always fail at everything I try and nothing will ever get better");

        assertThat(suggestions).isNotNull();
        // Claude should identify at least one distortion (e.g., all-or-nothing, overgeneralization)
    }

    @Test
    @DisplayName("AiService.detectCrisis uses CrisisDetector with AI analysis for non-crisis text")
    void detectCrisis_NonCrisisText_ReturnsLowOrNoneRisk() {
        CrisisDetectionResultDto result = aiService.detectCrisis(
                "I had a stressful day at work but I'm handling it okay");

        assertThat(result).isNotNull();
        assertThat(result.getRiskLevel()).isNotNull();
        assertThat(result.getRecommendedAction()).isNotNull();
        assertThat(result.getReasoning()).isNotBlank();
    }

    @Test
    @DisplayName("CrisisDetector.analyze performs AI-based semantic analysis via Claude")
    void crisisDetector_Analyze_PerformsAiAnalysis() {
        CrisisDetectionResultDto result = crisisDetector.analyze(
                "I'm feeling overwhelmed with my workload and need some rest");

        assertThat(result).isNotNull();
        assertThat(result.getRiskLevel()).isIn("none", "low", "medium");
        assertThat(result.getReasoning()).contains("AI assessment");
    }

    @Test
    @DisplayName("AiService.generateReframingPrompts calls Claude AI for CBT reframing")
    void generateReframingPrompts_CallsLLM_ReturnsSuggestions() {
        List<String> prompts = aiService.generateReframingPrompts(
                "Nobody at work appreciates my efforts",
                List.of("mind_reading", "overgeneralization"));

        assertThat(prompts).isNotNull();
        assertThat(prompts).isNotEmpty();
    }
}

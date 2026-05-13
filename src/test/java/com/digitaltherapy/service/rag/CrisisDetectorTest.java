package com.digitaltherapy.service.rag;

import com.digitaltherapy.dto.CrisisDetectionResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrisisDetectorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private CrisisDetector crisisDetector;

    @BeforeEach
    void setUp() {
        crisisDetector = new CrisisDetector(chatClient);
    }

    @Test
    void analyze_WithCrisisKeywords_ReturnsHighRisk() {
        String text = "I want to kill myself and hurt myself";
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("NONE");

        CrisisDetectionResultDto result = crisisDetector.analyze(text);

        assertThat(result.getRiskLevel()).isEqualTo("high");
        assertThat(result.getKeywordsDetected()).contains("kill myself", "hurt myself");
        assertThat(result.getRecommendedAction()).isEqualTo("show_crisis_hub");
    }

    @Test
    void analyze_WithMultipleCrisisKeywords_ReturnsCriticalRisk() {
        String text = "I want to kill myself, I want to end it all and I am better off dead";
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("NONE");

        CrisisDetectionResultDto result = crisisDetector.analyze(text);

        assertThat(result.getRiskLevel()).isEqualTo("critical");
        assertThat(result.getKeywordsDetected()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(result.getRecommendedAction()).isEqualTo("immediate_intervention");
    }

    @Test
    void analyze_NoCrisisKeywords_AiReturnsNone_ReturnsNone() {
        String text = "I had a good day at work today";
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("NONE");

        CrisisDetectionResultDto result = crisisDetector.analyze(text);

        assertThat(result.getRiskLevel()).isEqualTo("none");
        assertThat(result.getKeywordsDetected()).isEmpty();
        assertThat(result.getRecommendedAction()).isEqualTo("none");
    }

    @Test
    void analyze_NoCrisisKeywords_AiReturnsHigh_ReturnsHigh() {
        String text = "I feel like there is no point in anything anymore, everything is meaningless";
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("HIGH");

        CrisisDetectionResultDto result = crisisDetector.analyze(text);

        assertThat(result.getRiskLevel()).isEqualTo("high");
        assertThat(result.getKeywordsDetected()).isEmpty();
        assertThat(result.getRecommendedAction()).isEqualTo("show_crisis_hub");
    }

    @Test
    void analyze_AiFailure_FallsBackToKeywordOnly() {
        String text = "I want to kill myself";
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("API error"));

        CrisisDetectionResultDto result = crisisDetector.analyze(text);

        assertThat(result.getRiskLevel()).isEqualTo("medium");
        assertThat(result.getKeywordsDetected()).contains("kill myself");
        assertThat(result.getRecommendedAction()).isEqualTo("show_resources");
    }

    @Test
    void analyze_ReturnsProperRecommendedAction() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("NONE");

        // Verify CRITICAL -> IMMEDIATE_INTERVENTION
        String criticalText = "I want to kill myself and end it all and I am better off dead";
        CrisisDetectionResultDto criticalResult = crisisDetector.analyze(criticalText);
        assertThat(criticalResult.getRecommendedAction()).isEqualTo("immediate_intervention");

        // Verify HIGH -> SHOW_CRISIS_HUB
        String highText = "I want to kill myself and hurt myself";
        CrisisDetectionResultDto highResult = crisisDetector.analyze(highText);
        assertThat(highResult.getRecommendedAction()).isEqualTo("show_crisis_hub");

        // Verify MEDIUM -> SHOW_RESOURCES
        String mediumText = "I want to hurt myself";
        CrisisDetectionResultDto mediumResult = crisisDetector.analyze(mediumText);
        assertThat(mediumResult.getRecommendedAction()).isEqualTo("show_resources");
    }
}

package com.digitaltherapy.service.rag;

import com.digitaltherapy.dto.*;
import com.digitaltherapy.entity.*;
import com.digitaltherapy.exception.ResourceNotFoundException;
import com.digitaltherapy.repository.*;
import com.digitaltherapy.service.impl.AiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;
    @Mock
    private RagContextBuilder ragContextBuilder;
    @Mock
    private CrisisDetector crisisDetector;
    @Mock
    private VectorStore vectorStore;
    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private DiaryEntryRepository diaryEntryRepository;
    @Mock
    private CognitiveDistortionRepository cognitiveDistortionRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;

    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiServiceImpl(
                chatClient,
                ragContextBuilder,
                crisisDetector,
                vectorStore,
                userSessionRepository,
                diaryEntryRepository,
                cognitiveDistortionRepository,
                chatMessageRepository
        );
    }

    @Test
    void generateResponse_ReturnsAiChatResponse() {
        // given
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String userMessage = "I feel overwhelmed at work";

        User user = User.builder().id(userId).build();
        UserSession session = UserSession.builder().id(sessionId).user(user).build();

        when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(ragContextBuilder.buildContext(eq(userId), eq(sessionId), eq(userMessage)))
                .thenReturn("relevant context");
        when(chatClient.prompt().system(anyString()).user(eq(userMessage)).call().content())
                .thenReturn("I understand you feel overwhelmed. Let's explore that.");
        when(crisisDetector.analyze(userMessage))
                .thenReturn(CrisisDetectionResultDto.builder()
                        .riskLevel("none")
                        .keywordsDetected(Collections.emptyList())
                        .recommendedAction("none")
                        .reasoning("No crisis indicators detected.")
                        .build());

        // when
        ChatResponse response = aiService.generateResponse(sessionId, userMessage);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("I understand you feel overwhelmed. Let's explore that.");
        assertThat(response.getRole()).isEqualTo("assistant");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.isCrisisDetected()).isFalse();
        assertThat(response.getCrisisAction()).isNull();

        verify(ragContextBuilder).buildContext(userId, sessionId, userMessage);
        verify(crisisDetector).analyze(userMessage);
    }

    @Test
    void generateResponse_SessionNotFound_ThrowsException() {
        // given
        UUID sessionId = UUID.randomUUID();
        when(userSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> aiService.generateResponse(sessionId, "hello"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    void analyzeThought_ParsesJsonResponse() {
        // given
        String thought = "I always mess everything up";
        CognitiveDistortion distortion = CognitiveDistortion.builder()
                .id("overgeneralization")
                .name("Overgeneralization")
                .description("Making broad conclusions from a single event")
                .build();
        when(cognitiveDistortionRepository.findAll()).thenReturn(List.of(distortion));

        String jsonResponse = """
                [{"distortionId":"overgeneralization","name":"Overgeneralization","confidence":0.9,"reasoning":"Use of 'always' indicates overgeneralization"}]
                """;
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(jsonResponse);

        // when
        List<DistortionSuggestion> suggestions = aiService.analyzeThought(thought);

        // then
        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).getDistortionId()).isEqualTo("overgeneralization");
        assertThat(suggestions.get(0).getName()).isEqualTo("Overgeneralization");
        assertThat(suggestions.get(0).getConfidence()).isEqualTo(0.9);
        assertThat(suggestions.get(0).getReasoning()).contains("always");
    }

    @Test
    void analyzeThought_InvalidJson_ReturnsEmptyList() {
        // given
        String thought = "I always mess everything up";
        when(cognitiveDistortionRepository.findAll()).thenReturn(Collections.emptyList());
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("This is not valid JSON at all");

        // when
        List<DistortionSuggestion> suggestions = aiService.analyzeThought(thought);

        // then
        assertThat(suggestions).isEmpty();
    }

    @Test
    void generateReframingPrompts_ReturnsPromptList() {
        // given
        String thought = "I always fail at everything";
        List<String> distortionIds = List.of("overgeneralization");

        CognitiveDistortion distortion = CognitiveDistortion.builder()
                .id("overgeneralization")
                .name("Overgeneralization")
                .description("Making broad conclusions from a single event")
                .build();
        when(cognitiveDistortionRepository.findAllById(distortionIds)).thenReturn(List.of(distortion));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("CBT reframing technique")));

        String jsonResponse = """
                ["Can you think of a time when you succeeded?","What would you tell a friend in this situation?","Is 'always' really accurate?"]
                """;
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(jsonResponse);

        // when
        List<String> prompts = aiService.generateReframingPrompts(thought, distortionIds);

        // then
        assertThat(prompts).hasSize(3);
        assertThat(prompts).contains("Can you think of a time when you succeeded?");
    }

    @Test
    void generateReframingPrompts_ParseFailure_ReturnsFallbackPrompts() {
        // given
        String thought = "I am worthless";
        List<String> distortionIds = List.of("labeling");

        when(cognitiveDistortionRepository.findAllById(distortionIds)).thenReturn(Collections.emptyList());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("not valid json");

        // when
        List<String> prompts = aiService.generateReframingPrompts(thought, distortionIds);

        // then - should return the 3 fallback prompts
        assertThat(prompts).hasSize(3);
        assertThat(prompts.get(0)).contains("evidence");
        assertThat(prompts.get(1)).contains("friend");
        assertThat(prompts.get(2)).contains("alternative");
    }

    @Test
    void detectCrisis_DelegatesToCrisisDetector() {
        // given
        String text = "I feel very sad today";
        CrisisDetectionResultDto expectedResult = CrisisDetectionResultDto.builder()
                .riskLevel("none")
                .keywordsDetected(Collections.emptyList())
                .recommendedAction("none")
                .reasoning("No crisis indicators.")
                .build();
        when(crisisDetector.analyze(text)).thenReturn(expectedResult);

        // when
        CrisisDetectionResultDto result = aiService.detectCrisis(text);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(crisisDetector).analyze(text);
    }

    @Test
    void generateInsights_CombinesRepoDataAndAi() {
        // given
        UUID userId = UUID.randomUUID();

        List<Object[]> topDistortions = List.of(
                new Object[]{"overgeneralization", "Overgeneralization", 5L},
                new Object[]{"catastrophizing", "Catastrophizing", 3L}
        );
        when(diaryEntryRepository.findTopDistortionsByUserId(userId)).thenReturn(topDistortions);
        when(diaryEntryRepository.calculateAverageMoodImprovement(userId)).thenReturn(2.5);

        Page<DiaryEntry> page = new PageImpl<>(
                List.of(DiaryEntry.builder().build()),
                PageRequest.of(0, 1),
                10L
        );
        when(diaryEntryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
                eq(userId), any(PageRequest.class))).thenReturn(page);

        String jsonResponse = """
                {"patterns":["You tend to overgeneralize negative events","Catastrophizing is common in work situations"],"recommendations":["Practice thought challenging daily","Use the ABCDE model for negative thoughts"]}
                """;
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(jsonResponse);

        // when
        DiaryInsights insights = aiService.generateInsights(userId);

        // then
        assertThat(insights).isNotNull();
        assertThat(insights.getTotalEntries()).isEqualTo(10);
        assertThat(insights.getAverageMoodImprovement()).isEqualTo(2.5);
        assertThat(insights.getTopDistortions()).hasSize(2);
        assertThat(insights.getTopDistortions().get(0).getName()).isEqualTo("Overgeneralization");
        assertThat(insights.getTopDistortions().get(0).getCount()).isEqualTo(5L);
        assertThat(insights.getPatterns()).hasSize(2);
        assertThat(insights.getRecommendations()).hasSize(2);
    }

    @Test
    void summarizeSession_ReturnsSummary() {
        // given
        UUID sessionId = UUID.randomUUID();

        CbtSession cbtSession = CbtSession.builder().title("Managing Anxiety").build();
        UserSession session = UserSession.builder()
                .id(sessionId)
                .cbtSession(cbtSession)
                .status(SessionStatus.COMPLETED)
                .startedAt(LocalDateTime.of(2026, 2, 28, 10, 0))
                .endedAt(LocalDateTime.of(2026, 2, 28, 10, 45))
                .moodBefore(3)
                .moodAfter(7)
                .build();
        when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        ChatMessage msg1 = ChatMessage.builder()
                .role(MessageRole.USER)
                .content("I feel anxious")
                .timestamp(LocalDateTime.of(2026, 2, 28, 10, 1))
                .build();
        ChatMessage msg2 = ChatMessage.builder()
                .role(MessageRole.ASSISTANT)
                .content("Let's explore your anxiety")
                .timestamp(LocalDateTime.of(2026, 2, 28, 10, 2))
                .build();
        when(chatMessageRepository.findByUserSessionIdOrderByTimestampAsc(sessionId))
                .thenReturn(List.of(msg1, msg2));

        String jsonResponse = """
                {"summary":"The user explored anxiety triggers and learned coping strategies.","keyInsights":["Identified work deadlines as a trigger","Practiced deep breathing technique"]}
                """;
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(jsonResponse);

        // when
        SessionSummary summary = aiService.summarizeSession(sessionId);

        // then
        assertThat(summary).isNotNull();
        assertThat(summary.getSessionId()).isEqualTo(sessionId);
        assertThat(summary.getTitle()).isEqualTo("Managing Anxiety");
        assertThat(summary.getStatus()).isEqualTo("COMPLETED");
        assertThat(summary.getMoodBefore()).isEqualTo(3);
        assertThat(summary.getMoodAfter()).isEqualTo(7);
        assertThat(summary.getSummary()).contains("anxiety");
        assertThat(summary.getKeyInsights()).hasSize(2);
        assertThat(summary.getStartedAt()).isEqualTo(LocalDateTime.of(2026, 2, 28, 10, 0));
        assertThat(summary.getEndedAt()).isEqualTo(LocalDateTime.of(2026, 2, 28, 10, 45));
    }
}

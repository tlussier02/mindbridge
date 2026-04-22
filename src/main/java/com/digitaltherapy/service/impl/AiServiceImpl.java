package com.digitaltherapy.service.impl;

import com.digitaltherapy.dto.*;
import com.digitaltherapy.entity.*;
import com.digitaltherapy.exception.ResourceNotFoundException;
import com.digitaltherapy.repository.*;
import com.digitaltherapy.service.AiService;
import com.digitaltherapy.service.rag.CrisisDetector;
import com.digitaltherapy.service.rag.RagContextBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are a compassionate AI therapy assistant specializing in Cognitive Behavioral Therapy (CBT) for workplace burnout recovery. Your role is to:

            Maintain an empathetic, non-judgmental, and supportive tone
            Use Socratic questioning to help users explore their thoughts
            Guide users through CBT techniques like thought challenging
            Recognize and gently address cognitive distortions
            Celebrate progress and provide encouragement

            IMMEDIATELY recognize crisis indicators and respond appropriately

            Context about the user and relevant CBT knowledge:
            %s

            Remember:
            Never provide medical diagnoses
            Encourage professional help when appropriate
            Maintain appropriate boundaries
            Prioritize user safety above all else""";

    private final ChatClient chatClient;
    private final RagContextBuilder ragContextBuilder;
    private final CrisisDetector crisisDetector;
    private final VectorStore vectorStore;
    private final UserSessionRepository userSessionRepository;
    private final DiaryEntryRepository diaryEntryRepository;
    private final CognitiveDistortionRepository cognitiveDistortionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public ChatResponse generateResponse(UUID sessionId, String userMessage) {
        log.debug("Generating AI response for session: {}", sessionId);

        // 1. Get UserSession to find userId
        UserSession session = userSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        UUID userId = session.getUser().getId();

        // 2. Build context using RagContextBuilder
        String context = ragContextBuilder.buildContext(userId, sessionId, userMessage);

        // 3. Build system prompt
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, context);

        // 4. Call ChatClient with system prompt and user message
        String aiResponse = chatClient.prompt()
            .system(systemPrompt)
            .user(userMessage)
            .call()
            .content();

        // 5. Check for crisis in the user message
        CrisisDetectionResultDto crisisResult = crisisDetector.analyze(userMessage);
        boolean crisisDetected = !crisisResult.getRiskLevel().equals("none");

        // 6. Return ChatResponse
        return ChatResponse.builder()
            .message(aiResponse)
            .role("assistant")
            .timestamp(LocalDateTime.now())
            .crisisDetected(crisisDetected)
            .crisisAction(crisisDetected ? crisisResult.getRecommendedAction() : null)
            .build();
    }

    @Override
    @Cacheable(value = "thoughtAnalysis", key = "#automaticThought")
    public List<DistortionSuggestion> analyzeThought(String automaticThought) {
        log.debug("Analyzing thought for cognitive distortions: {}", automaticThought);

        // 1. Get all distortion names
        List<CognitiveDistortion> allDistortions = cognitiveDistortionRepository.findAll();
        String distortionNames = allDistortions.stream()
            .map(d -> d.getId() + ": " + d.getName() + " - " + d.getDescription())
            .collect(Collectors.joining("\n"));

        // 2. Build analysis prompt
        String prompt = String.format(
            "Analyze the following automatic thought for cognitive distortions.\n\n" +
            "Automatic Thought: \"%s\"\n\n" +
            "Available Cognitive Distortions:\n%s\n\n" +
            "Respond with a JSON array of objects, each containing:\n" +
            "- \"distortionId\": the distortion id\n" +
            "- \"name\": the distortion name\n" +
            "- \"confidence\": a number between 0 and 1 indicating confidence\n" +
            "- \"reasoning\": a brief explanation of why this distortion applies\n\n" +
            "Only include distortions that are present. Respond with ONLY valid JSON, no other text.",
            automaticThought, distortionNames);

        // 3. Call ChatClient
        String response = chatClient.prompt()
            .system("You are a CBT expert analyzing thoughts for cognitive distortions. Respond with only valid JSON.")
            .user(prompt)
            .call()
            .content();

        // 4. Parse JSON response
        try {
            ObjectMapper mapper = new ObjectMapper();
            String cleaned = cleanJsonResponse(response);
            List<DistortionSuggestion> suggestions = mapper.readValue(
                cleaned, new TypeReference<List<DistortionSuggestion>>() {});
            return suggestions;
        } catch (Exception e) {
            log.warn("Failed to parse AI distortion analysis response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @Cacheable(value = "reframingPrompts", key = "#thought + '-' + #distortionIds.toString()")
    public List<String> generateReframingPrompts(String thought, List<String> distortionIds) {
        log.debug("Generating reframing prompts for thought with distortions: {}", distortionIds);

        // Get distortion details
        List<CognitiveDistortion> distortions = cognitiveDistortionRepository.findAllById(distortionIds);
        String distortionInfo = distortions.stream()
            .map(d -> d.getName() + ": " + d.getDescription())
            .collect(Collectors.joining("\n"));

        // Search for relevant reframing strategies in the knowledge base
        List<Document> relevantDocs = vectorStore.similaritySearch(
            SearchRequest.builder().query(thought + " reframing").topK(2).build()
        );
        String cbtContext = relevantDocs.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n"));

        String prompt = String.format(
            "Generate 3-5 reframing prompts (Socratic questions or alternative perspectives) for the following thought.\n\n" +
            "Original Thought: \"%s\"\n\n" +
            "Identified Cognitive Distortions:\n%s\n\n" +
            "Relevant CBT Knowledge:\n%s\n\n" +
            "Provide reframing prompts as a JSON array of strings. Each prompt should be a question or " +
            "alternative perspective that helps the user challenge and reframe their thinking.\n" +
            "Respond with ONLY valid JSON, no other text.",
            thought, distortionInfo, cbtContext);

        String response = chatClient.prompt()
            .system("You are a CBT therapist helping a user reframe negative thoughts. Respond with only valid JSON.")
            .user(prompt)
            .call()
            .content();

        try {
            ObjectMapper mapper = new ObjectMapper();
            String cleaned = cleanJsonResponse(response);
            return mapper.readValue(cleaned, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse reframing prompts response: {}", e.getMessage());
            return List.of(
                "What evidence supports this thought, and what evidence contradicts it?",
                "How would you advise a close friend who had this same thought?",
                "Is there an alternative way to look at this situation?"
            );
        }
    }

    @Override
    public CrisisDetectionResultDto detectCrisis(String text) {
        log.debug("Performing crisis detection analysis");
        return crisisDetector.analyze(text);
    }

    @Override
    public DiaryInsights generateInsights(UUID userId) {
        log.debug("Generating diary insights for user: {}", userId);

        // 1. Get user's diary stats from repository
        List<Object[]> topDistortions = diaryEntryRepository.findTopDistortionsByUserId(userId);
        Double avgMoodImprovement = diaryEntryRepository.calculateAverageMoodImprovement(userId);

        long totalEntries = diaryEntryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
            userId, org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();

        // Build distortion frequency list
        List<DiaryInsights.DistortionFrequency> distortionFrequencies = topDistortions.stream()
            .limit(5)
            .map(row -> DiaryInsights.DistortionFrequency.builder()
                .distortionId((String) row[0])
                .name((String) row[1])
                .count((Long) row[2])
                .build())
            .collect(Collectors.toList());

        // 2. Build prompt for AI-generated patterns and recommendations
        String distortionSummary = distortionFrequencies.stream()
            .map(d -> d.getName() + " (" + d.getCount() + " occurrences)")
            .collect(Collectors.joining(", "));

        String prompt = String.format(
            "Based on the following diary analysis data, provide patterns and recommendations:\n\n" +
            "Total Diary Entries: %d\n" +
            "Average Mood Improvement: %.1f points\n" +
            "Most Common Cognitive Distortions: %s\n\n" +
            "Provide your response as a JSON object with two fields:\n" +
            "- \"patterns\": an array of 2-4 observed patterns in the user's thinking\n" +
            "- \"recommendations\": an array of 2-4 actionable recommendations\n\n" +
            "Respond with ONLY valid JSON, no other text.",
            totalEntries,
            avgMoodImprovement != null ? avgMoodImprovement : 0.0,
            distortionSummary.isEmpty() ? "None recorded yet" : distortionSummary);

        String response = chatClient.prompt()
            .system("You are a CBT expert providing insights based on a user's thought diary data. Respond with only valid JSON.")
            .user(prompt)
            .call()
            .content();

        List<String> patterns = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            String cleaned = cleanJsonResponse(response);
            Map<String, List<String>> parsed = mapper.readValue(
                cleaned, new TypeReference<Map<String, List<String>>>() {});
            patterns = parsed.getOrDefault("patterns", new ArrayList<>());
            recommendations = parsed.getOrDefault("recommendations", new ArrayList<>());
        } catch (Exception e) {
            log.warn("Failed to parse AI insights response: {}", e.getMessage());
            patterns = List.of("Continue tracking your thoughts to reveal patterns over time.");
            recommendations = List.of("Keep up the practice of recording your automatic thoughts.");
        }

        return DiaryInsights.builder()
            .totalEntries((int) totalEntries)
            .averageMoodImprovement(avgMoodImprovement != null ? avgMoodImprovement : 0.0)
            .topDistortions(distortionFrequencies)
            .patterns(patterns)
            .recommendations(recommendations)
            .build();
    }

    @Override
    public SessionSummary summarizeSession(UUID sessionId) {
        log.debug("Summarizing session: {}", sessionId);

        // 1. Get session with messages
        UserSession session = userSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        List<ChatMessage> messages = chatMessageRepository
            .findByUserSessionIdOrderByTimestampAsc(sessionId);

        // 2. Build prompt for summarization
        String transcript = messages.stream()
            .map(m -> String.format("[%s]: %s", m.getRole(), m.getContent()))
            .collect(Collectors.joining("\n"));

        String prompt = String.format(
            "Summarize the following therapy session transcript.\n\n" +
            "Session Title: %s\n" +
            "Mood Before: %d\n" +
            "Mood After: %d\n\n" +
            "Transcript:\n%s\n\n" +
            "Provide your response as a JSON object with two fields:\n" +
            "- \"summary\": a 2-3 sentence summary of the session\n" +
            "- \"keyInsights\": an array of 2-4 key insights or takeaways from the session\n\n" +
            "Respond with ONLY valid JSON, no other text.",
            session.getCbtSession() != null ? session.getCbtSession().getTitle() : "Therapy Session",
            session.getMoodBefore() != null ? session.getMoodBefore() : 0,
            session.getMoodAfter() != null ? session.getMoodAfter() : 0,
            transcript);

        // 3. Call ChatClient
        String response = chatClient.prompt()
            .system("You are a CBT therapist summarizing a therapy session. Respond with only valid JSON.")
            .user(prompt)
            .call()
            .content();

        String summary = "Session completed.";
        List<String> keyInsights = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            String cleaned = cleanJsonResponse(response);
            Map<String, Object> parsed = mapper.readValue(
                cleaned, new TypeReference<Map<String, Object>>() {});
            summary = (String) parsed.getOrDefault("summary", summary);
            Object insights = parsed.get("keyInsights");
            if (insights instanceof List<?>) {
                keyInsights = ((List<?>) insights).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to parse session summary response: {}", e.getMessage());
            keyInsights = List.of("Session data has been recorded for future reference.");
        }

        // 4. Return SessionSummary
        return SessionSummary.builder()
            .sessionId(sessionId)
            .title(session.getCbtSession() != null ? session.getCbtSession().getTitle() : "Therapy Session")
            .status(session.getStatus() != null ? session.getStatus().name() : "UNKNOWN")
            .startedAt(session.getStartedAt())
            .endedAt(session.getEndedAt())
            .moodBefore(session.getMoodBefore())
            .moodAfter(session.getMoodAfter())
            .summary(summary)
            .keyInsights(keyInsights)
            .build();
    }

    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
}

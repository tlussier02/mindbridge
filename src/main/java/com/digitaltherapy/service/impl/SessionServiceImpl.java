package com.digitaltherapy.service.impl;

import com.digitaltherapy.dto.*;
import com.digitaltherapy.entity.*;
import com.digitaltherapy.exception.ResourceNotFoundException;
import com.digitaltherapy.repository.*;
import com.digitaltherapy.service.SessionService;
import com.digitaltherapy.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionModuleRepository sessionModuleRepository;
    private final CbtSessionRepository cbtSessionRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiService aiService;
    private final VectorStore vectorStore;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "sessionLibrary", key = "'all'")
    public List<SessionModuleDto> getSessionLibrary(UUID userId) {
        log.info("Fetching session library for user: {}", userId);

        List<SessionModule> modules = sessionModuleRepository.findAllByOrderByOrderIndexAsc();

        return modules.stream()
                .map(this::mapToSessionModuleDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "sessionDetails", key = "#sessionId")
    public SessionDetail getSessionDetails(UUID sessionId) {
        log.info("Fetching session details for session: {}", sessionId);

        CbtSession session = cbtSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("CBT session not found with id: " + sessionId));

        return SessionDetail.builder()
                .id(session.getId())
                .title(session.getTitle())
                .description(session.getDescription())
                .durationMinutes(session.getDurationMinutes())
                .objectives(session.getObjectives())
                .modalities(session.getModalities() != null
                        ? session.getModalities().stream().map(Modality::name).collect(Collectors.toList())
                        : List.of())
                .moduleName(session.getModule() != null ? session.getModule().getName() : null)
                .build();
    }

    @Override
    @Transactional
    public ActiveSession startSession(UUID userId, UUID sessionId) {
        log.info("Starting session {} for user {}", sessionId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        CbtSession cbtSession = cbtSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("CBT session not found with id: " + sessionId));

        UserSession userSession = UserSession.builder()
                .user(user)
                .cbtSession(cbtSession)
                .status(SessionStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();

        UserSession saved = userSessionRepository.save(userSession);
        log.info("User session created with id: {}", saved.getId());

        return ActiveSession.builder()
                .sessionId(cbtSession.getId())
                .userSessionId(saved.getId())
                .title(cbtSession.getTitle())
                .description(cbtSession.getDescription())
                .startedAt(saved.getStartedAt())
                .moodBefore(saved.getMoodBefore())
                .build();
    }

    @Override
    @Transactional
    public ChatResponse chat(UUID sessionId, String message) {
        log.info("Processing chat message for user session: {}", sessionId);

        UserSession userSession = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("User session not found with id: " + sessionId));

        // Save the user message
        ChatMessage userMessage = ChatMessage.builder()
                .userSession(userSession)
                .role(MessageRole.USER)
                .content(message)
                .modality(Modality.TEXT)
                .build();
        chatMessageRepository.save(userMessage);

        // Generate AI response with fallback
        ChatResponse aiChatResponse;
        try {
            aiChatResponse = aiService.generateResponse(sessionId, message);
        } catch (Exception e) {
            log.warn("AI service unavailable, providing fallback response", e);
            aiChatResponse = ChatResponse.builder()
                    .message("I understand you said: \"" + message + "\". "
                            + "I'm currently experiencing a technical issue, but I'm here to support you. "
                            + "Please continue sharing your thoughts, and I'll do my best to help.")
                    .role(MessageRole.ASSISTANT.name())
                    .timestamp(LocalDateTime.now())
                    .crisisDetected(false)
                    .crisisAction(null)
                    .build();
        }

        // Save the assistant message
        ChatMessage assistantMessage = ChatMessage.builder()
                .userSession(userSession)
                .role(MessageRole.ASSISTANT)
                .content(aiChatResponse.getMessage())
                .modality(Modality.TEXT)
                .build();
        chatMessageRepository.save(assistantMessage);

        // Store conversation exchange in vector store for semantic retrieval
        storeMessageInVectorStore(userSession, userMessage);
        storeMessageInVectorStore(userSession, assistantMessage);

        return ChatResponse.builder()
                .message(aiChatResponse.getMessage())
                .role(MessageRole.ASSISTANT.name())
                .timestamp(assistantMessage.getTimestamp() != null
                        ? assistantMessage.getTimestamp()
                        : LocalDateTime.now())
                .crisisDetected(aiChatResponse.isCrisisDetected())
                .crisisAction(aiChatResponse.getCrisisAction())
                .build();
    }

    @Override
    @Transactional
    public SessionSummary endSession(UUID sessionId, String reason) {
        log.info("Ending user session: {} with reason: {}", sessionId, reason);

        UserSession userSession = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("User session not found with id: " + sessionId));

        SessionStatus status = "early_exit".equalsIgnoreCase(reason)
                ? SessionStatus.EARLY_EXIT
                : SessionStatus.COMPLETED;

        userSession.setStatus(status);
        userSession.setEndedAt(LocalDateTime.now());
        userSessionRepository.save(userSession);

        log.info("User session {} ended with status: {}", sessionId, status);

        // Store session summary in vector store
        storeSessionSummaryInVectorStore(userSession);

        CbtSession cbtSession = userSession.getCbtSession();

        return SessionSummary.builder()
                .sessionId(userSession.getId())
                .title(cbtSession != null ? cbtSession.getTitle() : "Session")
                .status(status.name())
                .startedAt(userSession.getStartedAt())
                .endedAt(userSession.getEndedAt())
                .moodBefore(userSession.getMoodBefore())
                .moodAfter(userSession.getMoodAfter())
                .summary("Session " + status.name().toLowerCase().replace("_", " ") + ".")
                .keyInsights(List.of())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionHistoryEntry> getSessionHistory(UUID userId) {
        log.info("Fetching session history for user: {}", userId);

        List<UserSession> sessions = userSessionRepository.findByUserIdOrderByStartedAtDesc(userId);

        return sessions.stream()
                .map(this::mapToSessionHistoryEntry)
                .collect(Collectors.toList());
    }

    private SessionModuleDto mapToSessionModuleDto(SessionModule module) {
        List<SessionModuleDto.SessionSummaryItem> sessionItems = module.getSessions() != null
                ? module.getSessions().stream()
                        .map(s -> SessionModuleDto.SessionSummaryItem.builder()
                                .id(s.getId())
                                .title(s.getTitle())
                                .durationMinutes(s.getDurationMinutes())
                                .build())
                        .collect(Collectors.toList())
                : new ArrayList<>();

        return SessionModuleDto.builder()
                .id(module.getId())
                .name(module.getName())
                .description(module.getDescription())
                .category(module.getCategory())
                .orderIndex(module.getOrderIndex())
                .sessions(sessionItems)
                .build();
    }

    private SessionHistoryEntry mapToSessionHistoryEntry(UserSession us) {
        CbtSession cbt = us.getCbtSession();
        return SessionHistoryEntry.builder()
                .id(us.getId())
                .sessionTitle(cbt != null ? cbt.getTitle() : "Unknown Session")
                .moduleName(cbt != null && cbt.getModule() != null ? cbt.getModule().getName() : null)
                .status(us.getStatus() != null ? us.getStatus().name() : null)
                .startedAt(us.getStartedAt())
                .endedAt(us.getEndedAt())
                .moodBefore(us.getMoodBefore())
                .moodAfter(us.getMoodAfter())
                .build();
    }

    private void storeMessageInVectorStore(UserSession userSession, ChatMessage message) {
        try {
            String userId = userSession.getUser().getId().toString();
            String sessionId = userSession.getId().toString();
            String sessionTitle = userSession.getCbtSession() != null
                    ? userSession.getCbtSession().getTitle() : "Session";

            String content = String.format("[%s] %s session '%s': %s",
                    message.getRole(), message.getRole() == MessageRole.USER ? "User in" : "Therapist in",
                    sessionTitle, message.getContent());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "session-message");
            metadata.put("userId", userId);
            metadata.put("sessionId", sessionId);
            metadata.put("role", message.getRole().name());
            metadata.put("sessionTitle", sessionTitle);

            String docId = "msg-" + message.getId();
            Document doc = new Document(docId, content, metadata);
            vectorStore.add(List.of(doc));
            log.debug("Stored {} message in vector store for session {}", message.getRole(), sessionId);
        } catch (Exception e) {
            log.warn("Failed to store message in vector store: {}", e.getMessage());
        }
    }

    private void storeSessionSummaryInVectorStore(UserSession userSession) {
        try {
            String userId = userSession.getUser().getId().toString();
            String sessionId = userSession.getId().toString();
            CbtSession cbtSession = userSession.getCbtSession();
            String sessionTitle = cbtSession != null ? cbtSession.getTitle() : "Session";

            List<ChatMessage> messages = chatMessageRepository
                    .findByUserSessionIdOrderByTimestampAsc(userSession.getId());

            StringBuilder content = new StringBuilder();
            content.append("Session Summary: ").append(sessionTitle).append("\n");
            content.append("Status: ").append(userSession.getStatus()).append("\n");
            content.append("Mood Before: ").append(userSession.getMoodBefore() != null ? userSession.getMoodBefore() : "N/A").append("\n");
            content.append("Mood After: ").append(userSession.getMoodAfter() != null ? userSession.getMoodAfter() : "N/A").append("\n");
            content.append("Messages: ").append(messages.size()).append("\n");

            // Include key conversation excerpts
            content.append("Conversation:\n");
            for (ChatMessage msg : messages) {
                content.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "session-summary");
            metadata.put("userId", userId);
            metadata.put("sessionId", sessionId);
            metadata.put("sessionTitle", sessionTitle);
            metadata.put("status", userSession.getStatus().name());

            String docId = "session-summary-" + sessionId;
            Document doc = new Document(docId, content.toString(), metadata);
            vectorStore.add(List.of(doc));
            log.info("Stored session summary in vector store for session {}", sessionId);
        } catch (Exception e) {
            log.warn("Failed to store session summary in vector store: {}", e.getMessage());
        }
    }
}

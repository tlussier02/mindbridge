package com.digitaltherapy.service.rag;

import com.digitaltherapy.entity.*;
import com.digitaltherapy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RagContextBuilder {

    private final VectorStore vectorStore;
    private final UserSessionRepository sessionRepository;
    private final DiaryEntryRepository diaryRepository;
    private final ChatMessageRepository chatMessageRepository;

    public String buildContext(UUID userId, UUID sessionId, String query) {
        StringBuilder context = new StringBuilder();

        // 1. Retrieve relevant CBT knowledge via vector similarity search
        List<Document> cbtDocs = vectorStore.similaritySearch(
            SearchRequest.builder().query(query).topK(3).build()
        );
        context.append("## Relevant CBT Knowledge\n");
        cbtDocs.forEach(doc -> context.append(doc.getText()).append("\n\n"));

        // 2. Retrieve relevant past session data via vector similarity search
        try {
            var filterBuilder = new FilterExpressionBuilder();
            List<Document> sessionDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(query)
                    .topK(5)
                    .filterExpression(filterBuilder.and(
                        filterBuilder.eq("type", "session-message"),
                        filterBuilder.eq("userId", userId.toString())
                    ).build())
                    .build()
            );
            if (!sessionDocs.isEmpty()) {
                context.append("## Relevant Past Session Conversations\n");
                sessionDocs.forEach(doc -> context.append(doc.getText()).append("\n"));
                context.append("\n");
            }
        } catch (Exception e) {
            log.debug("No past session data found in vector store: {}", e.getMessage());
        }

        // 3. Get user's recent session history
        List<UserSession> recentSessions = sessionRepository
            .findByUserIdOrderByStartedAtDesc(userId)
            .stream().limit(5).collect(Collectors.toList());
        context.append("## Recent Session History\n");
        recentSessions.forEach(s -> context.append(formatSession(s)).append("\n"));

        // 4. Get user's diary patterns
        List<DiaryEntry> recentEntries = diaryRepository
            .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
            .getContent();
        context.append("## Recent Diary Patterns\n");
        recentEntries.forEach(e -> context.append(formatEntry(e)).append("\n"));

        // 5. Get current session transcript
        if (sessionId != null) {
            List<ChatMessage> transcript = chatMessageRepository
                .findByUserSessionIdOrderByTimestampAsc(sessionId);
            context.append("## Current Session Transcript\n");
            transcript.forEach(m -> context.append(formatMessage(m)).append("\n"));
        }

        return context.toString();
    }

    private String formatSession(UserSession session) {
        return String.format("Session: %s | Status: %s | Mood: %d -> %d | Date: %s",
            session.getCbtSession() != null ? session.getCbtSession().getTitle() : "Unknown",
            session.getStatus(),
            session.getMoodBefore() != null ? session.getMoodBefore() : 0,
            session.getMoodAfter() != null ? session.getMoodAfter() : 0,
            session.getStartedAt());
    }

    private String formatEntry(DiaryEntry entry) {
        return String.format("Diary: %s | Thought: %s | Mood: %d -> %d",
            entry.getSituation(),
            entry.getAutomaticThought(),
            entry.getMoodBefore() != null ? entry.getMoodBefore() : 0,
            entry.getMoodAfter() != null ? entry.getMoodAfter() : 0);
    }

    private String formatMessage(ChatMessage message) {
        return String.format("[%s] %s: %s", message.getTimestamp(), message.getRole(), message.getContent());
    }
}

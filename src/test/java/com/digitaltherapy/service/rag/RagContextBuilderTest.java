package com.digitaltherapy.service.rag;

import com.digitaltherapy.entity.*;
import com.digitaltherapy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeast;

@ExtendWith(MockitoExtension.class)
class RagContextBuilderTest {

    @Mock
    private VectorStore vectorStore;
    @Mock
    private UserSessionRepository sessionRepository;
    @Mock
    private DiaryEntryRepository diaryRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;

    private RagContextBuilder ragContextBuilder;

    @BeforeEach
    void setUp() {
        ragContextBuilder = new RagContextBuilder(
                vectorStore,
                sessionRepository,
                diaryRepository,
                chatMessageRepository
        );
    }

    @Test
    void buildContext_IncludesCbtKnowledge() {
        // given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String query = "I feel anxious about work";

        Document cbtDoc = new Document("CBT technique: Thought challenging helps identify cognitive distortions.");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(cbtDoc));
        when(sessionRepository.findByUserIdOrderByStartedAtDesc(userId)).thenReturn(Collections.emptyList());

        Page<DiaryEntry> emptyPage = new PageImpl<>(Collections.emptyList());
        when(diaryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
                .thenReturn(emptyPage);
        when(chatMessageRepository.findByUserSessionIdOrderByTimestampAsc(sessionId))
                .thenReturn(Collections.emptyList());

        // when
        String context = ragContextBuilder.buildContext(userId, sessionId, query);

        // then
        assertThat(context).contains("Relevant CBT Knowledge");
        assertThat(context).contains("Thought challenging helps identify cognitive distortions");
        verify(vectorStore, atLeast(1)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void buildContext_IncludesSessionHistory() {
        // given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String query = "How am I doing?";

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        CbtSession cbtSession = CbtSession.builder().title("Intro to CBT").build();
        UserSession pastSession = UserSession.builder()
                .id(UUID.randomUUID())
                .cbtSession(cbtSession)
                .status(SessionStatus.COMPLETED)
                .moodBefore(3)
                .moodAfter(6)
                .startedAt(LocalDateTime.of(2026, 2, 27, 14, 0))
                .build();
        when(sessionRepository.findByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of(pastSession));

        Page<DiaryEntry> emptyPage = new PageImpl<>(Collections.emptyList());
        when(diaryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
                .thenReturn(emptyPage);
        when(chatMessageRepository.findByUserSessionIdOrderByTimestampAsc(sessionId))
                .thenReturn(Collections.emptyList());

        // when
        String context = ragContextBuilder.buildContext(userId, sessionId, query);

        // then
        assertThat(context).contains("Recent Session History");
        assertThat(context).contains("Intro to CBT");
        verify(sessionRepository).findByUserIdOrderByStartedAtDesc(userId);
    }

    @Test
    void buildContext_IncludesDiaryPatterns() {
        // given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String query = "My thoughts are negative";

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());
        when(sessionRepository.findByUserIdOrderByStartedAtDesc(userId)).thenReturn(Collections.emptyList());

        DiaryEntry entry = DiaryEntry.builder()
                .situation("Meeting at work")
                .automaticThought("I will fail the presentation")
                .moodBefore(3)
                .moodAfter(5)
                .build();
        Page<DiaryEntry> page = new PageImpl<>(List.of(entry));
        when(diaryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
                .thenReturn(page);
        when(chatMessageRepository.findByUserSessionIdOrderByTimestampAsc(sessionId))
                .thenReturn(Collections.emptyList());

        // when
        String context = ragContextBuilder.buildContext(userId, sessionId, query);

        // then
        assertThat(context).contains("Recent Diary Patterns");
        assertThat(context).contains("Meeting at work");
        assertThat(context).contains("I will fail the presentation");
        verify(diaryRepository).findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(userId), any(PageRequest.class));
    }

    @Test
    void buildContext_IncludesSessionTranscript() {
        // given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String query = "Tell me more";

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());
        when(sessionRepository.findByUserIdOrderByStartedAtDesc(userId)).thenReturn(Collections.emptyList());

        Page<DiaryEntry> emptyPage = new PageImpl<>(Collections.emptyList());
        when(diaryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
                .thenReturn(emptyPage);

        ChatMessage msg = ChatMessage.builder()
                .role(MessageRole.USER)
                .content("I feel stressed at work")
                .timestamp(LocalDateTime.of(2026, 2, 28, 10, 0))
                .build();
        when(chatMessageRepository.findByUserSessionIdOrderByTimestampAsc(sessionId))
                .thenReturn(List.of(msg));

        // when
        String context = ragContextBuilder.buildContext(userId, sessionId, query);

        // then
        assertThat(context).contains("Current Session Transcript");
        assertThat(context).contains("I feel stressed at work");
        verify(chatMessageRepository).findByUserSessionIdOrderByTimestampAsc(sessionId);
    }

    @Test
    void buildContext_NullSessionId_SkipsTranscript() {
        // given
        UUID userId = UUID.randomUUID();
        String query = "General question";

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());
        when(sessionRepository.findByUserIdOrderByStartedAtDesc(userId)).thenReturn(Collections.emptyList());

        Page<DiaryEntry> emptyPage = new PageImpl<>(Collections.emptyList());
        when(diaryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
                .thenReturn(emptyPage);

        // when - sessionId is null
        String context = ragContextBuilder.buildContext(userId, null, query);

        // then - transcript section header should not appear
        assertThat(context).doesNotContain("Current Session Transcript");
        verify(chatMessageRepository, never()).findByUserSessionIdOrderByTimestampAsc(any());
    }
}

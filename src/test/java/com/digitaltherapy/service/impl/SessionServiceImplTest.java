package com.digitaltherapy.service.impl;

import com.digitaltherapy.dto.*;
import com.digitaltherapy.entity.*;
import com.digitaltherapy.exception.ResourceNotFoundException;
import com.digitaltherapy.repository.*;
import com.digitaltherapy.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID TEST_SESSION_ID = UUID.randomUUID();
    private static final UUID TEST_USER_SESSION_ID = UUID.randomUUID();
    private static final UUID TEST_MODULE_ID = UUID.randomUUID();
    private static final UUID TEST_CBT_SESSION_ID = UUID.randomUUID();

    @Mock
    private SessionModuleRepository sessionModuleRepository;

    @Mock
    private CbtSessionRepository cbtSessionRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private AiService aiService;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private SessionServiceImpl sessionService;

    private User testUser;
    private SessionModule testModule;
    private CbtSession testCbtSession;
    private UserSession testUserSession;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .email("test@example.com")
                .passwordHash("encoded_password")
                .name("Test User")
                .streakDays(5)
                .build();

        testModule = SessionModule.builder()
                .id(TEST_MODULE_ID)
                .name("Stress Management")
                .description("Learn to manage stress effectively")
                .category("CBT")
                .orderIndex(1)
                .sessions(new ArrayList<>())
                .build();

        testCbtSession = CbtSession.builder()
                .id(TEST_CBT_SESSION_ID)
                .module(testModule)
                .title("Introduction to CBT")
                .description("Learn the basics of CBT")
                .durationMinutes(30)
                .objectives(List.of("Understand CBT", "Learn thought recording"))
                .modalities(List.of(Modality.TEXT, Modality.VOICE))
                .orderIndex(1)
                .build();

        testModule.getSessions().add(testCbtSession);

        testUserSession = UserSession.builder()
                .id(TEST_USER_SESSION_ID)
                .user(testUser)
                .cbtSession(testCbtSession)
                .status(SessionStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .moodBefore(5)
                .build();
    }

    @Test
    @DisplayName("getSessionLibrary - returns modules with sessions mapped to DTOs")
    void getSessionLibrary_ReturnsModulesWithSessions() {
        // Arrange
        when(sessionModuleRepository.findAllByOrderByOrderIndexAsc()).thenReturn(List.of(testModule));

        // Act
        List<SessionModuleDto> result = sessionService.getSessionLibrary(TEST_USER_ID);

        // Assert
        assertThat(result).hasSize(1);
        SessionModuleDto moduleDto = result.get(0);
        assertThat(moduleDto.getId()).isEqualTo(TEST_MODULE_ID);
        assertThat(moduleDto.getName()).isEqualTo("Stress Management");
        assertThat(moduleDto.getDescription()).isEqualTo("Learn to manage stress effectively");
        assertThat(moduleDto.getCategory()).isEqualTo("CBT");
        assertThat(moduleDto.getOrderIndex()).isEqualTo(1);
        assertThat(moduleDto.getSessions()).hasSize(1);

        SessionModuleDto.SessionSummaryItem sessionItem = moduleDto.getSessions().get(0);
        assertThat(sessionItem.getId()).isEqualTo(TEST_CBT_SESSION_ID);
        assertThat(sessionItem.getTitle()).isEqualTo("Introduction to CBT");
        assertThat(sessionItem.getDurationMinutes()).isEqualTo(30);

        verify(sessionModuleRepository).findAllByOrderByOrderIndexAsc();
    }

    @Test
    @DisplayName("getSessionDetails - found - returns session detail")
    void getSessionDetails_Found_ReturnsDetail() {
        // Arrange
        when(cbtSessionRepository.findById(TEST_CBT_SESSION_ID)).thenReturn(Optional.of(testCbtSession));

        // Act
        SessionDetail result = sessionService.getSessionDetails(TEST_CBT_SESSION_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_CBT_SESSION_ID);
        assertThat(result.getTitle()).isEqualTo("Introduction to CBT");
        assertThat(result.getDescription()).isEqualTo("Learn the basics of CBT");
        assertThat(result.getDurationMinutes()).isEqualTo(30);
        assertThat(result.getObjectives()).containsExactly("Understand CBT", "Learn thought recording");
        assertThat(result.getModalities()).containsExactly("TEXT", "VOICE");
        assertThat(result.getModuleName()).isEqualTo("Stress Management");

        verify(cbtSessionRepository).findById(TEST_CBT_SESSION_ID);
    }

    @Test
    @DisplayName("getSessionDetails - not found - throws ResourceNotFoundException")
    void getSessionDetails_NotFound_ThrowsException() {
        // Arrange
        UUID missingId = UUID.randomUUID();
        when(cbtSessionRepository.findById(missingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> sessionService.getSessionDetails(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CBT session not found");

        verify(cbtSessionRepository).findById(missingId);
    }

    @Test
    @DisplayName("startSession - success - creates UserSession and returns ActiveSession")
    void startSession_Success_ReturnsActiveSession() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(cbtSessionRepository.findById(TEST_CBT_SESSION_ID)).thenReturn(Optional.of(testCbtSession));
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(testUserSession);

        // Act
        ActiveSession result = sessionService.startSession(TEST_USER_ID, TEST_CBT_SESSION_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(TEST_CBT_SESSION_ID);
        assertThat(result.getUserSessionId()).isEqualTo(TEST_USER_SESSION_ID);
        assertThat(result.getTitle()).isEqualTo("Introduction to CBT");
        assertThat(result.getDescription()).isEqualTo("Learn the basics of CBT");
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getMoodBefore()).isEqualTo(5);

        verify(userRepository).findById(TEST_USER_ID);
        verify(cbtSessionRepository).findById(TEST_CBT_SESSION_ID);
        verify(userSessionRepository).save(any(UserSession.class));
    }

    @Test
    @DisplayName("chat - success - saves messages and returns AI response")
    void chat_Success_ReturnsAiResponse() {
        // Arrange
        String userMessage = "I feel anxious about work";
        ChatResponse aiResponse = ChatResponse.builder()
                .message("It sounds like you are experiencing work-related anxiety.")
                .role(MessageRole.ASSISTANT.name())
                .timestamp(LocalDateTime.now())
                .crisisDetected(false)
                .crisisAction(null)
                .build();

        when(userSessionRepository.findById(TEST_USER_SESSION_ID)).thenReturn(Optional.of(testUserSession));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiService.generateResponse(TEST_USER_SESSION_ID, userMessage)).thenReturn(aiResponse);

        // Act
        ChatResponse result = sessionService.chat(TEST_USER_SESSION_ID, userMessage);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("It sounds like you are experiencing work-related anxiety.");
        assertThat(result.getRole()).isEqualTo("ASSISTANT");
        assertThat(result.isCrisisDetected()).isFalse();
        assertThat(result.getCrisisAction()).isNull();

        verify(userSessionRepository).findById(TEST_USER_SESSION_ID);
        // User message saved + Assistant message saved = 2 saves
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
        verify(aiService).generateResponse(TEST_USER_SESSION_ID, userMessage);
    }

    @Test
    @DisplayName("chat - AI failure - returns fallback response")
    void chat_AiFailure_ReturnsFallback() {
        // Arrange
        String userMessage = "I need help";

        when(userSessionRepository.findById(TEST_USER_SESSION_ID)).thenReturn(Optional.of(testUserSession));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiService.generateResponse(TEST_USER_SESSION_ID, userMessage))
                .thenThrow(new RuntimeException("AI service unavailable"));

        // Act
        ChatResponse result = sessionService.chat(TEST_USER_SESSION_ID, userMessage);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMessage()).contains("I understand you said");
        assertThat(result.getMessage()).contains(userMessage);
        assertThat(result.getMessage()).contains("technical issue");
        assertThat(result.getRole()).isEqualTo("ASSISTANT");
        assertThat(result.isCrisisDetected()).isFalse();
        assertThat(result.getCrisisAction()).isNull();

        verify(aiService).generateResponse(TEST_USER_SESSION_ID, userMessage);
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("endSession - completed - marks session completed and returns summary")
    void endSession_Completed_ReturnsSummary() {
        // Arrange
        testUserSession.setMoodBefore(3);
        testUserSession.setMoodAfter(7);

        when(userSessionRepository.findById(TEST_USER_SESSION_ID)).thenReturn(Optional.of(testUserSession));
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(testUserSession);

        // Act
        SessionSummary result = sessionService.endSession(TEST_USER_SESSION_ID, "completed");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(TEST_USER_SESSION_ID);
        assertThat(result.getTitle()).isEqualTo("Introduction to CBT");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getEndedAt()).isNotNull();
        assertThat(result.getMoodBefore()).isEqualTo(3);
        assertThat(result.getMoodAfter()).isEqualTo(7);
        assertThat(result.getSummary()).isEqualTo("Session completed.");
        assertThat(result.getKeyInsights()).isEmpty();

        verify(userSessionRepository).findById(TEST_USER_SESSION_ID);
        verify(userSessionRepository).save(any(UserSession.class));
    }

    @Test
    @DisplayName("getSessionHistory - returns list of history entries")
    void getSessionHistory_ReturnsEntries() {
        // Arrange
        UserSession completedSession = UserSession.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .cbtSession(testCbtSession)
                .status(SessionStatus.COMPLETED)
                .startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().minusDays(1).plusMinutes(30))
                .moodBefore(4)
                .moodAfter(7)
                .build();

        when(userSessionRepository.findByUserIdOrderByStartedAtDesc(TEST_USER_ID))
                .thenReturn(List.of(testUserSession, completedSession));

        // Act
        List<SessionHistoryEntry> result = sessionService.getSessionHistory(TEST_USER_ID);

        // Assert
        assertThat(result).hasSize(2);

        SessionHistoryEntry first = result.get(0);
        assertThat(first.getId()).isEqualTo(TEST_USER_SESSION_ID);
        assertThat(first.getSessionTitle()).isEqualTo("Introduction to CBT");
        assertThat(first.getModuleName()).isEqualTo("Stress Management");
        assertThat(first.getStatus()).isEqualTo("IN_PROGRESS");

        SessionHistoryEntry second = result.get(1);
        assertThat(second.getStatus()).isEqualTo("COMPLETED");
        assertThat(second.getMoodBefore()).isEqualTo(4);
        assertThat(second.getMoodAfter()).isEqualTo(7);

        verify(userSessionRepository).findByUserIdOrderByStartedAtDesc(TEST_USER_ID);
    }
}

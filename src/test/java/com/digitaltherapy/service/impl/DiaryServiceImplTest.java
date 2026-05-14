package com.digitaltherapy.service.impl;

import com.digitaltherapy.dto.*;
import com.digitaltherapy.entity.CognitiveDistortion;
import com.digitaltherapy.entity.DiaryEntry;
import com.digitaltherapy.entity.EmotionRating;
import com.digitaltherapy.entity.User;
import com.digitaltherapy.exception.ResourceNotFoundException;
import com.digitaltherapy.repository.CognitiveDistortionRepository;
import com.digitaltherapy.repository.DiaryEntryRepository;
import com.digitaltherapy.repository.UserRepository;
import com.digitaltherapy.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryServiceImplTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID TEST_ENTRY_ID = UUID.randomUUID();

    @Mock
    private DiaryEntryRepository diaryEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CognitiveDistortionRepository cognitiveDistortionRepository;

    @Mock
    private AiService aiService;

    @InjectMocks
    private DiaryServiceImpl diaryService;

    private User testUser;
    private DiaryEntry testEntry;
    private CognitiveDistortion testDistortion;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .email("test@example.com")
                .passwordHash("encoded_password")
                .name("Test User")
                .streakDays(5)
                .build();

        testDistortion = CognitiveDistortion.builder()
                .id("all_or_nothing")
                .name("All-or-Nothing Thinking")
                .description("Seeing things in black or white categories")
                .examples(List.of("I always fail", "Nothing ever works"))
                .build();

        testEntry = DiaryEntry.builder()
                .id(TEST_ENTRY_ID)
                .user(testUser)
                .situation("Stressful meeting at work")
                .automaticThought("I will fail the presentation")
                .emotions(List.of(
                        EmotionRating.builder().emotion("anxiety").intensity(8).build(),
                        EmotionRating.builder().emotion("fear").intensity(6).build()
                ))
                .distortions(List.of(testDistortion))
                .alternativeThought("I have prepared well and can handle this")
                .moodBefore(3)
                .moodAfter(6)
                .beliefRatingBefore(80)
                .beliefRatingAfter(40)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("createEntry - success - returns DiaryEntryResponse with mapped fields")
    void createEntry_Success_ReturnsDiaryEntryResponse() {
        // Arrange
        DiaryEntryCreate request = DiaryEntryCreate.builder()
                .situation("Stressful meeting at work")
                .automaticThought("I will fail the presentation")
                .emotions(List.of(
                        DiaryEntryCreate.EmotionRatingDto.builder()
                                .emotion("anxiety").intensity(8).build(),
                        DiaryEntryCreate.EmotionRatingDto.builder()
                                .emotion("fear").intensity(6).build()
                ))
                .distortionIds(List.of("all_or_nothing"))
                .alternativeThought("I have prepared well and can handle this")
                .moodBefore(3)
                .moodAfter(6)
                .beliefRatingBefore(80)
                .beliefRatingAfter(40)
                .build();

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(cognitiveDistortionRepository.findAllById(List.of("all_or_nothing")))
                .thenReturn(List.of(testDistortion));
        when(diaryEntryRepository.save(any(DiaryEntry.class))).thenReturn(testEntry);

        // Act
        DiaryEntryResponse result = diaryService.createEntry(TEST_USER_ID, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_ENTRY_ID);
        assertThat(result.getSituation()).isEqualTo("Stressful meeting at work");
        assertThat(result.getAutomaticThought()).isEqualTo("I will fail the presentation");
        assertThat(result.getEmotions()).hasSize(2);
        assertThat(result.getEmotions().get(0).getEmotion()).isEqualTo("anxiety");
        assertThat(result.getEmotions().get(0).getIntensity()).isEqualTo(8);
        assertThat(result.getDistortionIds()).containsExactly("all_or_nothing");
        assertThat(result.getAlternativeThought()).isEqualTo("I have prepared well and can handle this");
        assertThat(result.getMoodBefore()).isEqualTo(3);
        assertThat(result.getMoodAfter()).isEqualTo(6);
        assertThat(result.getBeliefRatingBefore()).isEqualTo(80);
        assertThat(result.getBeliefRatingAfter()).isEqualTo(40);

        verify(userRepository).findById(TEST_USER_ID);
        verify(cognitiveDistortionRepository).findAllById(List.of("all_or_nothing"));
        verify(diaryEntryRepository).save(any(DiaryEntry.class));
    }

    @Test
    @DisplayName("createEntry - user not found - throws ResourceNotFoundException")
    void createEntry_UserNotFound_ThrowsException() {
        // Arrange
        UUID missingUserId = UUID.randomUUID();
        DiaryEntryCreate request = DiaryEntryCreate.builder()
                .situation("Some situation")
                .automaticThought("Some thought")
                .build();

        when(userRepository.findById(missingUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> diaryService.createEntry(missingUserId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(missingUserId);
        verify(diaryEntryRepository, never()).save(any(DiaryEntry.class));
    }

    @Test
    @DisplayName("getEntries - returns paged summaries")
    void getEntries_ReturnsPagedSummaries() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<DiaryEntry> page = new PageImpl<>(List.of(testEntry), pageable, 1);

        when(diaryEntryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(TEST_USER_ID, pageable))
                .thenReturn(page);

        // Act
        Page<DiaryEntrySummary> result = diaryService.getEntries(TEST_USER_ID, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);

        DiaryEntrySummary summary = result.getContent().get(0);
        assertThat(summary.getId()).isEqualTo(TEST_ENTRY_ID);
        assertThat(summary.getSituation()).isEqualTo("Stressful meeting at work");
        assertThat(summary.getAutomaticThought()).isEqualTo("I will fail the presentation");
        assertThat(summary.getMoodBefore()).isEqualTo(3);
        assertThat(summary.getMoodAfter()).isEqualTo(6);
        assertThat(summary.getDistortionCount()).isEqualTo(1);

        verify(diaryEntryRepository).findByUserIdAndDeletedFalseOrderByCreatedAtDesc(TEST_USER_ID, pageable);
    }

    @Test
    @DisplayName("getEntryDetail - found - returns full detail")
    void getEntryDetail_Found_ReturnsDetail() {
        // Arrange
        when(diaryEntryRepository.findByIdAndUserIdAndDeletedFalse(TEST_ENTRY_ID, TEST_USER_ID))
                .thenReturn(Optional.of(testEntry));

        // Act
        DiaryEntryDetail result = diaryService.getEntryDetail(TEST_USER_ID, TEST_ENTRY_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_ENTRY_ID);
        assertThat(result.getSituation()).isEqualTo("Stressful meeting at work");
        assertThat(result.getAutomaticThought()).isEqualTo("I will fail the presentation");
        assertThat(result.getEmotions()).hasSize(2);
        assertThat(result.getEmotions().get(0).getEmotion()).isEqualTo("anxiety");
        assertThat(result.getDistortions()).hasSize(1);
        assertThat(result.getDistortions().get(0).getId()).isEqualTo("all_or_nothing");
        assertThat(result.getDistortions().get(0).getName()).isEqualTo("All-or-Nothing Thinking");
        assertThat(result.getDistortions().get(0).getDescription()).isEqualTo("Seeing things in black or white categories");
        assertThat(result.getAlternativeThought()).isEqualTo("I have prepared well and can handle this");
        assertThat(result.getMoodBefore()).isEqualTo(3);
        assertThat(result.getMoodAfter()).isEqualTo(6);
        assertThat(result.getBeliefRatingBefore()).isEqualTo(80);
        assertThat(result.getBeliefRatingAfter()).isEqualTo(40);

        verify(diaryEntryRepository).findByIdAndUserIdAndDeletedFalse(TEST_ENTRY_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("getEntryDetail - not found - throws ResourceNotFoundException")
    void getEntryDetail_NotFound_ThrowsException() {
        // Arrange
        UUID missingId = UUID.randomUUID();
        when(diaryEntryRepository.findByIdAndUserIdAndDeletedFalse(missingId, TEST_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> diaryService.getEntryDetail(TEST_USER_ID, missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Diary entry not found");

        verify(diaryEntryRepository).findByIdAndUserIdAndDeletedFalse(missingId, TEST_USER_ID);
    }

    @Test
    @DisplayName("deleteEntry - soft deletes the entry")
    void deleteEntry_SoftDeletes() {
        // Arrange
        when(diaryEntryRepository.findByIdAndUserIdAndDeletedFalse(TEST_ENTRY_ID, TEST_USER_ID))
                .thenReturn(Optional.of(testEntry));
        when(diaryEntryRepository.save(any(DiaryEntry.class))).thenReturn(testEntry);

        // Act
        diaryService.deleteEntry(TEST_USER_ID, TEST_ENTRY_ID);

        // Assert
        assertThat(testEntry.getDeleted()).isTrue();

        verify(diaryEntryRepository).findByIdAndUserIdAndDeletedFalse(TEST_ENTRY_ID, TEST_USER_ID);
        verify(diaryEntryRepository).save(testEntry);
    }

    @Test
    @DisplayName("suggestDistortions - returns AI suggestions")
    void suggestDistortions_ReturnsAiSuggestions() {
        // Arrange
        String thought = "I always mess everything up";
        List<DistortionSuggestion> suggestions = List.of(
                DistortionSuggestion.builder()
                        .distortionId("all_or_nothing")
                        .name("All-or-Nothing Thinking")
                        .confidence(0.92)
                        .reasoning("Use of 'always' and 'everything' indicates black-and-white thinking")
                        .build(),
                DistortionSuggestion.builder()
                        .distortionId("overgeneralization")
                        .name("Overgeneralization")
                        .confidence(0.85)
                        .reasoning("Generalizing from a single event to all situations")
                        .build()
        );

        when(aiService.analyzeThought(thought)).thenReturn(suggestions);

        // Act
        List<DistortionSuggestion> result = diaryService.suggestDistortions(thought);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDistortionId()).isEqualTo("all_or_nothing");
        assertThat(result.get(0).getConfidence()).isEqualTo(0.92);
        assertThat(result.get(1).getDistortionId()).isEqualTo("overgeneralization");

        verify(aiService).analyzeThought(thought);
    }

    @Test
    @DisplayName("suggestDistortions - AI failure - returns empty list")
    void suggestDistortions_AiFailure_ReturnsEmptyList() {
        // Arrange
        String thought = "I always mess everything up";
        when(aiService.analyzeThought(thought)).thenThrow(new RuntimeException("AI service unavailable"));

        // Act
        List<DistortionSuggestion> result = diaryService.suggestDistortions(thought);

        // Assert
        assertThat(result).isEmpty();

        verify(aiService).analyzeThought(thought);
    }

    @Test
    @DisplayName("getInsights - returns insights with repository data and AI patterns")
    void getInsights_ReturnsInsights() {
        // Arrange
        Page<DiaryEntry> allEntries = new PageImpl<>(List.of(testEntry));

        Object[] distortionRow = new Object[]{"all_or_nothing", "All-or-Nothing Thinking", 5L};
        List<Object[]> topDistortionData = new java.util.ArrayList<>();
        topDistortionData.add(distortionRow);

        DiaryInsights aiInsights = DiaryInsights.builder()
                .patterns(List.of("You tend to catastrophize in work situations"))
                .recommendations(List.of("Practice thought challenging before meetings"))
                .build();

        when(diaryEntryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(allEntries);
        when(diaryEntryRepository.calculateAverageMoodImprovement(TEST_USER_ID)).thenReturn(2.5);
        when(diaryEntryRepository.findTopDistortionsByUserId(TEST_USER_ID)).thenReturn(topDistortionData);
        when(aiService.generateInsights(TEST_USER_ID)).thenReturn(aiInsights);

        // Act
        DiaryInsights result = diaryService.getInsights(TEST_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalEntries()).isEqualTo(1);
        assertThat(result.getAverageMoodImprovement()).isEqualTo(2.5);
        assertThat(result.getTopDistortions()).hasSize(1);
        assertThat(result.getTopDistortions().get(0).getDistortionId()).isEqualTo("all_or_nothing");
        assertThat(result.getTopDistortions().get(0).getName()).isEqualTo("All-or-Nothing Thinking");
        assertThat(result.getTopDistortions().get(0).getCount()).isEqualTo(5L);
        assertThat(result.getPatterns()).containsExactly("You tend to catastrophize in work situations");
        assertThat(result.getRecommendations()).containsExactly("Practice thought challenging before meetings");

        verify(diaryEntryRepository).findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(TEST_USER_ID), any(Pageable.class));
        verify(diaryEntryRepository).calculateAverageMoodImprovement(TEST_USER_ID);
        verify(diaryEntryRepository).findTopDistortionsByUserId(TEST_USER_ID);
        verify(aiService).generateInsights(TEST_USER_ID);
    }
}

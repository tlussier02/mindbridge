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
import com.digitaltherapy.service.DiaryService;
import com.digitaltherapy.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryEntryRepository diaryEntryRepository;
    private final UserRepository userRepository;
    private final CognitiveDistortionRepository cognitiveDistortionRepository;
    private final AiService aiService;

    @Override
    @Transactional
    public DiaryEntryResponse createEntry(UUID userId, DiaryEntryCreate request) {
        log.info("Creating diary entry for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Map emotion DTOs to embeddable entities
        List<EmotionRating> emotions = new ArrayList<>();
        if (request.getEmotions() != null) {
            emotions = request.getEmotions().stream()
                    .map(e -> EmotionRating.builder()
                            .emotion(e.getEmotion())
                            .intensity(e.getIntensity())
                            .build())
                    .collect(Collectors.toList());
        }

        // Lookup cognitive distortions by IDs
        List<CognitiveDistortion> distortions = new ArrayList<>();
        if (request.getDistortionIds() != null && !request.getDistortionIds().isEmpty()) {
            distortions = cognitiveDistortionRepository.findAllById(request.getDistortionIds());
        }

        DiaryEntry entry = DiaryEntry.builder()
                .user(user)
                .situation(request.getSituation())
                .automaticThought(request.getAutomaticThought())
                .emotions(emotions)
                .distortions(distortions)
                .alternativeThought(request.getAlternativeThought())
                .moodBefore(request.getMoodBefore())
                .moodAfter(request.getMoodAfter())
                .beliefRatingBefore(request.getBeliefRatingBefore())
                .beliefRatingAfter(request.getBeliefRatingAfter())
                .deleted(false)
                .build();

        DiaryEntry saved = diaryEntryRepository.save(entry);
        log.info("Diary entry created with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
<<<<<<< HEAD
=======
    @Transactional(readOnly = true)
>>>>>>> 2bb2ef62b9902fd4c36412ff39432e6f45bb2bf3
    public Page<DiaryEntrySummary> getEntries(UUID userId, Pageable pageable) {
        log.info("Fetching diary entries for user: {}", userId);

        Page<DiaryEntry> entries = diaryEntryRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable);

        return entries.map(this::mapToSummary);
    }

    @Override
<<<<<<< HEAD
=======
    @Transactional(readOnly = true)
>>>>>>> 2bb2ef62b9902fd4c36412ff39432e6f45bb2bf3
    public DiaryEntryDetail getEntryDetail(UUID entryId) {
        log.info("Fetching diary entry detail for entry: {}", entryId);

        DiaryEntry entry = diaryEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Diary entry not found with id: " + entryId));

        return mapToDetail(entry);
    }

    @Override
    @Transactional
    public void deleteEntry(UUID entryId) {
        log.info("Soft deleting diary entry: {}", entryId);

        DiaryEntry entry = diaryEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Diary entry not found with id: " + entryId));

        entry.setDeleted(true);
        diaryEntryRepository.save(entry);

        log.info("Diary entry {} soft deleted", entryId);
    }

    @Override
    public List<DistortionSuggestion> suggestDistortions(String thought) {
        log.info("Suggesting distortions for thought");

        try {
            return aiService.analyzeThought(thought);
        } catch (Exception e) {
            log.warn("AI service unavailable for distortion suggestion, returning empty list", e);
            return List.of();
        }
    }

    @Override
    public DiaryInsights getInsights(UUID userId) {
        log.info("Generating diary insights for user: {}", userId);

        // Calculate stats from repository
        Page<DiaryEntry> allEntries = diaryEntryRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, Pageable.unpaged());
        int totalEntries = (int) allEntries.getTotalElements();

        Double avgMoodImprovement = diaryEntryRepository.calculateAverageMoodImprovement(userId);
        double moodImprovement = avgMoodImprovement != null ? avgMoodImprovement : 0.0;

        // Get top distortions
        List<Object[]> topDistortionData = diaryEntryRepository.findTopDistortionsByUserId(userId);
        List<DiaryInsights.DistortionFrequency> topDistortions = topDistortionData.stream()
                .map(row -> DiaryInsights.DistortionFrequency.builder()
                        .distortionId((String) row[0])
                        .name((String) row[1])
                        .count((Long) row[2])
                        .build())
                .collect(Collectors.toList());

        // Attempt AI-generated insights
        List<String> patterns = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        try {
            DiaryInsights aiInsights = aiService.generateInsights(userId);
            if (aiInsights != null) {
                if (aiInsights.getPatterns() != null) {
                    patterns = aiInsights.getPatterns();
                }
                if (aiInsights.getRecommendations() != null) {
                    recommendations = aiInsights.getRecommendations();
                }
            }
        } catch (Exception e) {
            log.warn("AI service unavailable for insights generation, using defaults", e);
            patterns = List.of("Continue tracking your thoughts to identify patterns.");
            recommendations = List.of("Keep up your journaling practice for best results.");
        }

        return DiaryInsights.builder()
                .totalEntries(totalEntries)
                .averageMoodImprovement(moodImprovement)
                .topDistortions(topDistortions)
                .patterns(patterns)
                .recommendations(recommendations)
                .build();
    }

    private DiaryEntryResponse mapToResponse(DiaryEntry entry) {
        List<DiaryEntryResponse.EmotionRatingDto> emotions = entry.getEmotions() != null
                ? entry.getEmotions().stream()
                        .map(e -> DiaryEntryResponse.EmotionRatingDto.builder()
                                .emotion(e.getEmotion())
                                .intensity(e.getIntensity())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        List<String> distortionIds = entry.getDistortions() != null
                ? entry.getDistortions().stream()
                        .map(CognitiveDistortion::getId)
                        .collect(Collectors.toList())
                : List.of();

        return DiaryEntryResponse.builder()
                .id(entry.getId())
                .situation(entry.getSituation())
                .automaticThought(entry.getAutomaticThought())
                .emotions(emotions)
                .distortionIds(distortionIds)
                .alternativeThought(entry.getAlternativeThought())
                .moodBefore(entry.getMoodBefore())
                .moodAfter(entry.getMoodAfter())
                .beliefRatingBefore(entry.getBeliefRatingBefore())
                .beliefRatingAfter(entry.getBeliefRatingAfter())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private DiaryEntrySummary mapToSummary(DiaryEntry entry) {
        return DiaryEntrySummary.builder()
                .id(entry.getId())
                .situation(entry.getSituation())
                .automaticThought(entry.getAutomaticThought())
                .moodBefore(entry.getMoodBefore())
                .moodAfter(entry.getMoodAfter())
                .createdAt(entry.getCreatedAt())
                .distortionCount(entry.getDistortions() != null ? entry.getDistortions().size() : 0)
                .build();
    }

    private DiaryEntryDetail mapToDetail(DiaryEntry entry) {
        List<DiaryEntryDetail.EmotionInfo> emotions = entry.getEmotions() != null
                ? entry.getEmotions().stream()
                        .map(e -> DiaryEntryDetail.EmotionInfo.builder()
                                .emotion(e.getEmotion())
                                .intensity(e.getIntensity())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        List<DiaryEntryDetail.DistortionInfo> distortions = entry.getDistortions() != null
                ? entry.getDistortions().stream()
                        .map(d -> DiaryEntryDetail.DistortionInfo.builder()
                                .id(d.getId())
                                .name(d.getName())
                                .description(d.getDescription())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        return DiaryEntryDetail.builder()
                .id(entry.getId())
                .situation(entry.getSituation())
                .automaticThought(entry.getAutomaticThought())
                .emotions(emotions)
                .distortions(distortions)
                .alternativeThought(entry.getAlternativeThought())
                .moodBefore(entry.getMoodBefore())
                .moodAfter(entry.getMoodAfter())
                .beliefRatingBefore(entry.getBeliefRatingBefore())
                .beliefRatingAfter(entry.getBeliefRatingAfter())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}

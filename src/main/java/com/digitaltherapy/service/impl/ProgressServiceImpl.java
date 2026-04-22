package com.digitaltherapy.service.impl;

import com.digitaltherapy.dto.*;
import com.digitaltherapy.entity.DiaryEntry;
import com.digitaltherapy.entity.User;
import com.digitaltherapy.entity.UserSession;
import com.digitaltherapy.entity.SessionStatus;
import com.digitaltherapy.exception.ResourceNotFoundException;
import com.digitaltherapy.repository.DiaryEntryRepository;
import com.digitaltherapy.repository.UserRepository;
import com.digitaltherapy.repository.UserSessionRepository;
import com.digitaltherapy.service.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private final UserSessionRepository userSessionRepository;
    private final DiaryEntryRepository diaryEntryRepository;
    private final UserRepository userRepository;

    @Override
    public WeeklyProgress getWeeklyProgress(UUID userId) {
        log.info("Calculating weekly progress for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        LocalDateTime startDateTime = weekStart.atStartOfDay();
        LocalDateTime endDateTime = weekEnd.plusDays(1).atStartOfDay();

        // Sessions completed this week
        List<UserSession> weekSessions = userSessionRepository
                .findByUserIdAndDateRange(userId, startDateTime, endDateTime);
        int sessionsCompleted = (int) weekSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .count();

        // Diary entries this week
        Page<DiaryEntry> allEntries = diaryEntryRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, Pageable.unpaged());
        List<DiaryEntry> weekEntries = allEntries.getContent().stream()
                .filter(e -> e.getCreatedAt() != null
                        && !e.getCreatedAt().isBefore(startDateTime)
                        && e.getCreatedAt().isBefore(endDateTime))
                .collect(Collectors.toList());

        // Average mood from diary entries this week
        double averageMood = weekEntries.stream()
                .filter(e -> e.getMoodAfter() != null)
                .mapToInt(DiaryEntry::getMoodAfter)
                .average()
                .orElse(0.0);

        // Build daily mood breakdown
        List<WeeklyProgress.DailyMood> dailyMoods = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

            List<DiaryEntry> dayEntries = weekEntries.stream()
                    .filter(e -> e.getCreatedAt() != null
                            && !e.getCreatedAt().isBefore(dayStart)
                            && e.getCreatedAt().isBefore(dayEnd))
                    .collect(Collectors.toList());

            double dayAvgMood = dayEntries.stream()
                    .filter(e -> e.getMoodAfter() != null)
                    .mapToInt(DiaryEntry::getMoodAfter)
                    .average()
                    .orElse(0.0);

            dailyMoods.add(WeeklyProgress.DailyMood.builder()
                    .date(day)
                    .averageMood(dayAvgMood)
                    .entriesCount(dayEntries.size())
                    .build());
        }

        return WeeklyProgress.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .sessionsCompleted(sessionsCompleted)
                .diaryEntries(weekEntries.size())
                .averageMood(averageMood)
                .streakDays(user.getStreakDays() != null ? user.getStreakDays() : 0)
                .dailyMoods(dailyMoods)
                .build();
    }

    @Override
    public MonthlyTrend getMonthlyTrend(UUID userId) {
        log.info("Calculating monthly trend for user: {}", userId);

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        LocalDateTime startDateTime = monthStart.atStartOfDay();
        LocalDateTime endDateTime = monthEnd.plusDays(1).atStartOfDay();

        // Sessions this month
        List<UserSession> monthSessions = userSessionRepository
                .findByUserIdAndDateRange(userId, startDateTime, endDateTime);
        int totalSessions = (int) monthSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .count();

        // Diary entries this month
        Page<DiaryEntry> allEntries = diaryEntryRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, Pageable.unpaged());
        List<DiaryEntry> monthEntries = allEntries.getContent().stream()
                .filter(e -> e.getCreatedAt() != null
                        && !e.getCreatedAt().isBefore(startDateTime)
                        && e.getCreatedAt().isBefore(endDateTime))
                .collect(Collectors.toList());

        // Calculate mood averages
        double avgMoodStart = monthEntries.stream()
                .filter(e -> e.getMoodBefore() != null)
                .mapToInt(DiaryEntry::getMoodBefore)
                .average()
                .orElse(0.0);

        double avgMoodEnd = monthEntries.stream()
                .filter(e -> e.getMoodAfter() != null)
                .mapToInt(DiaryEntry::getMoodAfter)
                .average()
                .orElse(0.0);

        // Build weekly breakdown within the month
        List<MonthlyTrend.WeeklySummaryItem> weeks = new ArrayList<>();
        LocalDate weekCursor = monthStart;
        int weekNum = 1;

        while (!weekCursor.isAfter(monthEnd)) {
            LocalDate weekEndDate = weekCursor.plusDays(6);
            if (weekEndDate.isAfter(monthEnd)) {
                weekEndDate = monthEnd;
            }

            LocalDateTime wStart = weekCursor.atStartOfDay();
            LocalDateTime wEnd = weekEndDate.plusDays(1).atStartOfDay();

            int wSessions = (int) monthSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.COMPLETED
                            && s.getStartedAt() != null
                            && !s.getStartedAt().isBefore(wStart)
                            && s.getStartedAt().isBefore(wEnd))
                    .count();

            List<DiaryEntry> wEntries = monthEntries.stream()
                    .filter(e -> e.getCreatedAt() != null
                            && !e.getCreatedAt().isBefore(wStart)
                            && e.getCreatedAt().isBefore(wEnd))
                    .collect(Collectors.toList());

            double wAvgMood = wEntries.stream()
                    .filter(e -> e.getMoodAfter() != null)
                    .mapToInt(DiaryEntry::getMoodAfter)
                    .average()
                    .orElse(0.0);

            weeks.add(MonthlyTrend.WeeklySummaryItem.builder()
                    .weekNumber(weekNum)
                    .sessions(wSessions)
                    .entries(wEntries.size())
                    .avgMood(wAvgMood)
                    .build());

            weekCursor = weekEndDate.plusDays(1);
            weekNum++;
        }

        return MonthlyTrend.builder()
                .month(today.getMonth().name())
                .year(today.getYear())
                .totalSessions(totalSessions)
                .totalDiaryEntries(monthEntries.size())
                .averageMoodStart(avgMoodStart)
                .averageMoodEnd(avgMoodEnd)
                .moodTrend(avgMoodEnd - avgMoodStart)
                .weeks(weeks)
                .build();
    }

    @Override
    public BurnoutRecovery getBurnoutRecovery(UUID userId) {
        log.info("Calculating burnout recovery for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Long completedSessions = userSessionRepository.countCompletedByUserId(userId);
        int sessionCount = completedSessions != null ? completedSessions.intValue() : 0;

        Page<DiaryEntry> allEntries = diaryEntryRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, Pageable.unpaged());
        int entryCount = (int) allEntries.getTotalElements();

        Double avgMoodImprovement = diaryEntryRepository.calculateAverageMoodImprovement(userId);
        double moodImprovement = avgMoodImprovement != null ? avgMoodImprovement : 0.0;

        // Calculate recovery score (0-100) based on activity and mood improvement
        double activityScore = Math.min(sessionCount * 5.0, 40.0);
        double journalScore = Math.min(entryCount * 3.0, 30.0);
        double moodScore = Math.min(Math.max(moodImprovement * 10.0, 0.0), 30.0);
        double recoveryScore = activityScore + journalScore + moodScore;

        // Determine overall status
        String overallStatus;
        if (recoveryScore >= 80) {
            overallStatus = "THRIVING";
        } else if (recoveryScore >= 60) {
            overallStatus = "RECOVERING";
        } else if (recoveryScore >= 30) {
            overallStatus = "IN_PROGRESS";
        } else {
            overallStatus = "STARTING";
        }

        // Build dimension scores
        Map<String, Double> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("emotional_awareness", Math.min(entryCount * 5.0, 100.0));
        dimensionScores.put("cbt_engagement", Math.min(sessionCount * 10.0, 100.0));
        dimensionScores.put("mood_improvement", Math.min(Math.max(moodImprovement * 20.0, 0.0), 100.0));
        dimensionScores.put("consistency", Math.min(
                (user.getStreakDays() != null ? user.getStreakDays() : 0) * 14.3, 100.0));

        int consecutiveDays = user.getStreakDays() != null ? user.getStreakDays() : 0;

        // Build recommendations
        List<String> recommendations = new ArrayList<>();
        if (sessionCount < 3) {
            recommendations.add("Try completing more CBT sessions to build your coping skills.");
        }
        if (entryCount < 5) {
            recommendations.add("Regular journaling helps identify thought patterns. Aim for daily entries.");
        }
        if (moodImprovement < 1.0) {
            recommendations.add("Focus on practicing alternative thoughts in your diary entries.");
        }
        if (consecutiveDays < 3) {
            recommendations.add("Build a daily habit by engaging with the app each day.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Great progress! Keep maintaining your current routine.");
        }

        return BurnoutRecovery.builder()
                .overallStatus(overallStatus)
                .recoveryScore(recoveryScore)
                .dimensionScores(dimensionScores)
                .recommendations(recommendations)
                .consecutiveDaysActive(consecutiveDays)
                .build();
    }

    @Override
    public List<Achievement> getAchievements(UUID userId) {
        log.info("Fetching achievements for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Long completedSessions = userSessionRepository.countCompletedByUserId(userId);
        int sessionCount = completedSessions != null ? completedSessions.intValue() : 0;

        Page<DiaryEntry> allEntries = diaryEntryRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, Pageable.unpaged());
        int entryCount = (int) allEntries.getTotalElements();

        int streakDays = user.getStreakDays() != null ? user.getStreakDays() : 0;

        List<Achievement> achievements = new ArrayList<>();

        // First Session
        achievements.add(Achievement.builder()
                .id("first_session")
                .name("First Step")
                .description("Complete your first CBT session")
                .icon("rocket")
                .unlocked(sessionCount >= 1)
                .unlockedAt(sessionCount >= 1 ? user.getCreatedAt() : null)
                .progress(Math.min(sessionCount, 1))
                .build());

        // 5 Sessions
        achievements.add(Achievement.builder()
                .id("five_sessions")
                .name("Dedicated Learner")
                .description("Complete 5 CBT sessions")
                .icon("book")
                .unlocked(sessionCount >= 5)
                .unlockedAt(sessionCount >= 5 ? user.getUpdatedAt() : null)
                .progress(Math.min(sessionCount / 5.0, 1.0))
                .build());

        // 10 Sessions
        achievements.add(Achievement.builder()
                .id("ten_sessions")
                .name("CBT Expert")
                .description("Complete 10 CBT sessions")
                .icon("star")
                .unlocked(sessionCount >= 10)
                .unlockedAt(sessionCount >= 10 ? user.getUpdatedAt() : null)
                .progress(Math.min(sessionCount / 10.0, 1.0))
                .build());

        // First Diary Entry
        achievements.add(Achievement.builder()
                .id("first_diary")
                .name("Thought Tracker")
                .description("Write your first diary entry")
                .icon("pencil")
                .unlocked(entryCount >= 1)
                .unlockedAt(entryCount >= 1 ? user.getCreatedAt() : null)
                .progress(Math.min(entryCount, 1))
                .build());

        // 10 Diary Entries
        achievements.add(Achievement.builder()
                .id("ten_diaries")
                .name("Reflective Mind")
                .description("Write 10 diary entries")
                .icon("journal")
                .unlocked(entryCount >= 10)
                .unlockedAt(entryCount >= 10 ? user.getUpdatedAt() : null)
                .progress(Math.min(entryCount / 10.0, 1.0))
                .build());

        // 3-Day Streak
        achievements.add(Achievement.builder()
                .id("three_day_streak")
                .name("Building Momentum")
                .description("Maintain a 3-day streak")
                .icon("fire")
                .unlocked(streakDays >= 3)
                .unlockedAt(streakDays >= 3 ? user.getUpdatedAt() : null)
                .progress(Math.min(streakDays / 3.0, 1.0))
                .build());

        // 7-Day Streak
        achievements.add(Achievement.builder()
                .id("seven_day_streak")
                .name("Week Warrior")
                .description("Maintain a 7-day streak")
                .icon("trophy")
                .unlocked(streakDays >= 7)
                .unlockedAt(streakDays >= 7 ? user.getUpdatedAt() : null)
                .progress(Math.min(streakDays / 7.0, 1.0))
                .build());

        // 30-Day Streak
        achievements.add(Achievement.builder()
                .id("thirty_day_streak")
                .name("Mental Health Champion")
                .description("Maintain a 30-day streak")
                .icon("crown")
                .unlocked(streakDays >= 30)
                .unlockedAt(streakDays >= 30 ? user.getUpdatedAt() : null)
                .progress(Math.min(streakDays / 30.0, 1.0))
                .build());

        return achievements;
    }
}

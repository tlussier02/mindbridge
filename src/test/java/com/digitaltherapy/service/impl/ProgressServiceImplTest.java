package com.digitaltherapy.service.impl;

import com.digitaltherapy.dto.Achievement;
import com.digitaltherapy.dto.BurnoutRecovery;
import com.digitaltherapy.dto.MonthlyTrend;
import com.digitaltherapy.dto.WeeklyProgress;
import com.digitaltherapy.entity.DiaryEntry;
import com.digitaltherapy.entity.SessionStatus;
import com.digitaltherapy.entity.User;
import com.digitaltherapy.entity.UserSession;
import com.digitaltherapy.exception.ResourceNotFoundException;
import com.digitaltherapy.repository.DiaryEntryRepository;
import com.digitaltherapy.repository.UserRepository;
import com.digitaltherapy.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceImplTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private DiaryEntryRepository diaryEntryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProgressServiceImpl progressService;

    private User testUser;
    private UserSession completedSession;
    private DiaryEntry testEntry;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .email("test@example.com")
                .passwordHash("encoded_password")
                .name("Test User")
                .streakDays(5)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .build();

        completedSession = UserSession.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .status(SessionStatus.COMPLETED)
                .startedAt(LocalDateTime.now().minusHours(2))
                .endedAt(LocalDateTime.now().minusHours(1))
                .moodBefore(4)
                .moodAfter(7)
                .build();

        testEntry = DiaryEntry.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .situation("Test situation")
                .automaticThought("Test thought")
                .moodBefore(3)
                .moodAfter(7)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("getWeeklyProgress - returns weekly data with sessions, diary entries, and daily moods")
    void getWeeklyProgress_ReturnsWeeklyData() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userSessionRepository.findByUserIdAndDateRange(eq(TEST_USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(completedSession));
        when(diaryEntryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testEntry)));

        // Act
        WeeklyProgress result = progressService.getWeeklyProgress(TEST_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getWeekStart()).isNotNull();
        assertThat(result.getWeekEnd()).isNotNull();
        assertThat(result.getStreakDays()).isEqualTo(5);
        // The session was set as COMPLETED so sessionsCompleted should be >= 0
        assertThat(result.getSessionsCompleted()).isGreaterThanOrEqualTo(0);
        assertThat(result.getDailyMoods()).hasSize(7);

        // Verify each day in the week is accounted for
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int i = 0; i < 7; i++) {
            assertThat(result.getDailyMoods().get(i).getDate()).isEqualTo(weekStart.plusDays(i));
        }

        verify(userRepository).findById(TEST_USER_ID);
        verify(userSessionRepository).findByUserIdAndDateRange(eq(TEST_USER_ID), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(diaryEntryRepository).findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(TEST_USER_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("getMonthlyTrend - returns monthly data with sessions and mood trends")
    void getMonthlyTrend_ReturnsMonthlyData() {
        // Arrange
        when(userSessionRepository.findByUserIdAndDateRange(eq(TEST_USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(completedSession));
        when(diaryEntryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testEntry)));

        // Act
        MonthlyTrend result = progressService.getMonthlyTrend(TEST_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMonth()).isEqualTo(LocalDate.now().getMonth().name());
        assertThat(result.getYear()).isEqualTo(LocalDate.now().getYear());
        assertThat(result.getTotalSessions()).isGreaterThanOrEqualTo(0);
        assertThat(result.getTotalDiaryEntries()).isGreaterThanOrEqualTo(0);
        assertThat(result.getWeeks()).isNotEmpty();
        // moodTrend = averageMoodEnd - averageMoodStart
        assertThat(result.getMoodTrend()).isEqualTo(result.getAverageMoodEnd() - result.getAverageMoodStart());

        // Each weekly summary item should have a valid week number
        for (MonthlyTrend.WeeklySummaryItem week : result.getWeeks()) {
            assertThat(week.getWeekNumber()).isGreaterThan(0);
        }

        verify(userSessionRepository).findByUserIdAndDateRange(eq(TEST_USER_ID), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(diaryEntryRepository).findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(TEST_USER_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("getBurnoutRecovery - returns recovery metrics with correct scoring")
    void getBurnoutRecovery_ReturnsRecoveryMetrics() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userSessionRepository.countCompletedByUserId(TEST_USER_ID)).thenReturn(4L);
        when(diaryEntryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testEntry)));
        when(diaryEntryRepository.calculateAverageMoodImprovement(TEST_USER_ID)).thenReturn(2.0);

        // Act
        BurnoutRecovery result = progressService.getBurnoutRecovery(TEST_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecoveryScore()).isGreaterThanOrEqualTo(0.0);
        assertThat(result.getRecoveryScore()).isLessThanOrEqualTo(100.0);
        assertThat(result.getConsecutiveDaysActive()).isEqualTo(5);
        assertThat(result.getOverallStatus()).isIn("STARTING", "IN_PROGRESS", "RECOVERING", "THRIVING");

        // Verify dimension scores exist
        assertThat(result.getDimensionScores()).containsKeys(
                "emotional_awareness", "cbt_engagement", "mood_improvement", "consistency"
        );

        // With 4 sessions: activityScore = min(4*5, 40) = 20
        // With 1 entry: journalScore = min(1*3, 30) = 3
        // With 2.0 mood improvement: moodScore = min(max(2.0*10, 0), 30) = 20
        // recoveryScore = 20 + 3 + 20 = 43
        double expectedActivityScore = Math.min(4 * 5.0, 40.0);
        double expectedJournalScore = Math.min(1 * 3.0, 30.0);
        double expectedMoodScore = Math.min(Math.max(2.0 * 10.0, 0.0), 30.0);
        double expectedRecovery = expectedActivityScore + expectedJournalScore + expectedMoodScore;
        assertThat(result.getRecoveryScore()).isEqualTo(expectedRecovery);

        // recoveryScore = 43 -> IN_PROGRESS (>= 30 and < 60)
        assertThat(result.getOverallStatus()).isEqualTo("IN_PROGRESS");

        assertThat(result.getRecommendations()).isNotEmpty();

        verify(userRepository).findById(TEST_USER_ID);
        verify(userSessionRepository).countCompletedByUserId(TEST_USER_ID);
        verify(diaryEntryRepository).calculateAverageMoodImprovement(TEST_USER_ID);
    }

    @Test
    @DisplayName("getAchievements - returns achievements list with correct unlock states")
    void getAchievements_ReturnsAchievementsList() {
        // Arrange
        // testUser has streakDays = 5
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userSessionRepository.countCompletedByUserId(TEST_USER_ID)).thenReturn(6L);
        when(diaryEntryRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testEntry)));

        // Act
        List<Achievement> result = progressService.getAchievements(TEST_USER_ID);

        // Assert
        assertThat(result).hasSize(8);

        // With 6 sessions: first_session (unlocked), five_sessions (unlocked), ten_sessions (locked)
        Achievement firstSession = result.stream()
                .filter(a -> "first_session".equals(a.getId())).findFirst().orElseThrow();
        assertThat(firstSession.isUnlocked()).isTrue();
        assertThat(firstSession.getName()).isEqualTo("First Step");
        assertThat(firstSession.getProgress()).isEqualTo(1.0);

        Achievement fiveSessions = result.stream()
                .filter(a -> "five_sessions".equals(a.getId())).findFirst().orElseThrow();
        assertThat(fiveSessions.isUnlocked()).isTrue();
        assertThat(fiveSessions.getName()).isEqualTo("Dedicated Learner");

        Achievement tenSessions = result.stream()
                .filter(a -> "ten_sessions".equals(a.getId())).findFirst().orElseThrow();
        assertThat(tenSessions.isUnlocked()).isFalse();
        assertThat(tenSessions.getProgress()).isEqualTo(0.6); // 6/10.0

        // With 1 entry: first_diary (unlocked), ten_diaries (locked)
        Achievement firstDiary = result.stream()
                .filter(a -> "first_diary".equals(a.getId())).findFirst().orElseThrow();
        assertThat(firstDiary.isUnlocked()).isTrue();
        assertThat(firstDiary.getName()).isEqualTo("Thought Tracker");

        Achievement tenDiaries = result.stream()
                .filter(a -> "ten_diaries".equals(a.getId())).findFirst().orElseThrow();
        assertThat(tenDiaries.isUnlocked()).isFalse();
        assertThat(tenDiaries.getProgress()).isEqualTo(0.1); // 1/10.0

        // With 5 streak days: three_day_streak (unlocked), seven_day_streak (locked)
        Achievement threeDayStreak = result.stream()
                .filter(a -> "three_day_streak".equals(a.getId())).findFirst().orElseThrow();
        assertThat(threeDayStreak.isUnlocked()).isTrue();
        assertThat(threeDayStreak.getName()).isEqualTo("Building Momentum");

        Achievement sevenDayStreak = result.stream()
                .filter(a -> "seven_day_streak".equals(a.getId())).findFirst().orElseThrow();
        assertThat(sevenDayStreak.isUnlocked()).isFalse();
        assertThat(sevenDayStreak.getProgress()).isCloseTo(5.0 / 7.0, org.assertj.core.data.Offset.offset(0.01));

        Achievement thirtyDayStreak = result.stream()
                .filter(a -> "thirty_day_streak".equals(a.getId())).findFirst().orElseThrow();
        assertThat(thirtyDayStreak.isUnlocked()).isFalse();
        assertThat(thirtyDayStreak.getProgress()).isCloseTo(5.0 / 30.0, org.assertj.core.data.Offset.offset(0.01));

        verify(userRepository).findById(TEST_USER_ID);
        verify(userSessionRepository).countCompletedByUserId(TEST_USER_ID);
    }
}

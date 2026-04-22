package com.digitaltherapy.controller;

import com.digitaltherapy.config.JwtTokenProvider;
import com.digitaltherapy.dto.*;
import com.digitaltherapy.entity.User;
import com.digitaltherapy.repository.UserRepository;
import com.digitaltherapy.service.ProgressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgressController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgressService progressService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private ObjectMapper objectMapper;

    private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        User mockUser = User.builder().id(TEST_USER_ID).name("Test User").email("test@example.com").build();
        var auth = new UsernamePasswordAuthenticationToken(mockUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------- getWeeklyProgress
    @Test
    void getWeeklyProgress_Returns200() throws Exception {
        WeeklyProgress progress = WeeklyProgress.builder()
                .weekStart(LocalDate.of(2026, 2, 23))
                .weekEnd(LocalDate.of(2026, 2, 28))
                .sessionsCompleted(3)
                .diaryEntries(5)
                .averageMood(6.2)
                .streakDays(4)
                .dailyMoods(List.of(
                        WeeklyProgress.DailyMood.builder()
                                .date(LocalDate.of(2026, 2, 23))
                                .averageMood(5.5)
                                .entriesCount(2)
                                .build(),
                        WeeklyProgress.DailyMood.builder()
                                .date(LocalDate.of(2026, 2, 24))
                                .averageMood(6.8)
                                .entriesCount(1)
                                .build()
                ))
                .build();

        when(progressService.getWeeklyProgress(any(UUID.class))).thenReturn(progress);

        mockMvc.perform(get("/progress/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-02-23"))
                .andExpect(jsonPath("$.weekEnd").value("2026-02-28"))
                .andExpect(jsonPath("$.sessionsCompleted").value(3))
                .andExpect(jsonPath("$.diaryEntries").value(5))
                .andExpect(jsonPath("$.averageMood").value(6.2))
                .andExpect(jsonPath("$.streakDays").value(4))
                .andExpect(jsonPath("$.dailyMoods[0].date").value("2026-02-23"))
                .andExpect(jsonPath("$.dailyMoods[0].averageMood").value(5.5))
                .andExpect(jsonPath("$.dailyMoods[0].entriesCount").value(2))
                .andExpect(jsonPath("$.dailyMoods[1].date").value("2026-02-24"))
                .andExpect(jsonPath("$.dailyMoods[1].averageMood").value(6.8));
    }

    // --------------------------------------------------------- getMonthlyTrend
    @Test
    void getMonthlyTrend_Returns200() throws Exception {
        MonthlyTrend trend = MonthlyTrend.builder()
                .month("February")
                .year(2026)
                .totalSessions(12)
                .totalDiaryEntries(20)
                .averageMoodStart(4.0)
                .averageMoodEnd(6.5)
                .moodTrend(2.5)
                .weeks(List.of(
                        MonthlyTrend.WeeklySummaryItem.builder()
                                .weekNumber(1)
                                .sessions(3)
                                .entries(5)
                                .avgMood(4.5)
                                .build(),
                        MonthlyTrend.WeeklySummaryItem.builder()
                                .weekNumber(2)
                                .sessions(4)
                                .entries(6)
                                .avgMood(5.5)
                                .build()
                ))
                .build();

        when(progressService.getMonthlyTrend(any(UUID.class))).thenReturn(trend);

        mockMvc.perform(get("/progress/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("February"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.totalSessions").value(12))
                .andExpect(jsonPath("$.totalDiaryEntries").value(20))
                .andExpect(jsonPath("$.averageMoodStart").value(4.0))
                .andExpect(jsonPath("$.averageMoodEnd").value(6.5))
                .andExpect(jsonPath("$.moodTrend").value(2.5))
                .andExpect(jsonPath("$.weeks[0].weekNumber").value(1))
                .andExpect(jsonPath("$.weeks[0].sessions").value(3))
                .andExpect(jsonPath("$.weeks[0].entries").value(5))
                .andExpect(jsonPath("$.weeks[0].avgMood").value(4.5))
                .andExpect(jsonPath("$.weeks[1].weekNumber").value(2));
    }

    // ------------------------------------------------------- getBurnoutRecovery
    @Test
    void getBurnoutRecovery_Returns200() throws Exception {
        BurnoutRecovery recovery = BurnoutRecovery.builder()
                .overallStatus("RECOVERING")
                .recoveryScore(65.0)
                .dimensionScores(Map.of(
                        "emotional_exhaustion", 45.0,
                        "depersonalization", 30.0,
                        "personal_accomplishment", 70.0
                ))
                .recommendations(List.of(
                        "Continue daily journaling",
                        "Practice boundaries at work"
                ))
                .consecutiveDaysActive(7)
                .build();

        when(progressService.getBurnoutRecovery(any(UUID.class))).thenReturn(recovery);

        mockMvc.perform(get("/progress/burnout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("RECOVERING"))
                .andExpect(jsonPath("$.recoveryScore").value(65.0))
                .andExpect(jsonPath("$.dimensionScores.emotional_exhaustion").value(45.0))
                .andExpect(jsonPath("$.dimensionScores.depersonalization").value(30.0))
                .andExpect(jsonPath("$.dimensionScores.personal_accomplishment").value(70.0))
                .andExpect(jsonPath("$.recommendations[0]").value("Continue daily journaling"))
                .andExpect(jsonPath("$.recommendations[1]").value("Practice boundaries at work"))
                .andExpect(jsonPath("$.consecutiveDaysActive").value(7));
    }

    // -------------------------------------------------------- getAchievements
    @Test
    void getAchievements_Returns200() throws Exception {
        Achievement achievement1 = Achievement.builder()
                .id("first_session")
                .name("First Step")
                .description("Completed your first therapy session")
                .icon("star")
                .unlocked(true)
                .unlockedAt(LocalDateTime.of(2026, 2, 20, 12, 0, 0))
                .progress(1.0)
                .build();

        Achievement achievement2 = Achievement.builder()
                .id("diary_streak_7")
                .name("Week Warrior")
                .description("Maintained a 7-day diary streak")
                .icon("fire")
                .unlocked(false)
                .unlockedAt(null)
                .progress(0.57)
                .build();

        when(progressService.getAchievements(any(UUID.class)))
                .thenReturn(List.of(achievement1, achievement2));

        mockMvc.perform(get("/progress/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("first_session"))
                .andExpect(jsonPath("$[0].name").value("First Step"))
                .andExpect(jsonPath("$[0].description").value("Completed your first therapy session"))
                .andExpect(jsonPath("$[0].icon").value("star"))
                .andExpect(jsonPath("$[0].unlocked").value(true))
                .andExpect(jsonPath("$[0].progress").value(1.0))
                .andExpect(jsonPath("$[1].id").value("diary_streak_7"))
                .andExpect(jsonPath("$[1].name").value("Week Warrior"))
                .andExpect(jsonPath("$[1].unlocked").value(false))
                .andExpect(jsonPath("$[1].progress").value(0.57));
    }
}

package com.digitaltherapy.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyProgress {

    private LocalDate weekStart;
    private LocalDate weekEnd;
    private int sessionsCompleted;
    private int diaryEntries;
    private double averageMood;
    private int streakDays;
    private List<DailyMood> dailyMoods;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMood {

        private LocalDate date;
        private double averageMood;
        private int entriesCount;
    }
}

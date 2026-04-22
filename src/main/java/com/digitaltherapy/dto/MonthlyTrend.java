package com.digitaltherapy.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrend {

    private String month;
    private int year;
    private int totalSessions;
    private int totalDiaryEntries;
    private double averageMoodStart;
    private double averageMoodEnd;
    private double moodTrend;
    private List<WeeklySummaryItem> weeks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklySummaryItem {

        private int weekNumber;
        private int sessions;
        private int entries;
        private double avgMood;
    }
}

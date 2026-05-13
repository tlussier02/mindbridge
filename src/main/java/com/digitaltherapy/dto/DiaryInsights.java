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
public class DiaryInsights {

    private int totalEntries;
    private double averageMoodImprovement;
    private List<DistortionFrequency> topDistortions;
    private List<String> patterns;
    private List<String> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistortionFrequency {

        private String distortionId;
        private String name;
        private long count;
    }
}

package com.digitaltherapy.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BurnoutRecovery {

    private String overallStatus;
    private double recoveryScore;
    private Map<String, Double> dimensionScores;
    private List<String> recommendations;
    private int consecutiveDaysActive;
}

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
public class CrisisDetectionResultDto {

    private String riskLevel;
    private List<String> keywordsDetected;
    private String recommendedAction;
    private String reasoning;
}

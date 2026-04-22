package com.digitaltherapy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistortionSuggestion {

    private String distortionId;
    private String name;
    private double confidence;
    private String reasoning;
}

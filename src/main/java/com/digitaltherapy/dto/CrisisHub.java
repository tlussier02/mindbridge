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
public class CrisisHub {

    private String message;
    private List<EmergencyResource> emergencyResources;
    private List<String> copingStrategies;
    private String safetyPlanSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyResource {

        private String name;
        private String phone;
        private String description;
        private boolean available24x7;
    }
}

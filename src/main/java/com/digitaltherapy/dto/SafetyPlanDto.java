package com.digitaltherapy.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyPlanDto {

    private UUID userId;
    private List<String> warningSignals;
    private List<String> copingStrategies;
    private List<TrustedContactDto> trustedContacts;
    private List<String> professionalContacts;
    private List<String> environmentSafetySteps;
    private String reasonForLiving;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrustedContactDto {

        private String name;
        private String phone;
        private String relationship;
    }
}

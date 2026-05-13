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
public class SafetyPlanUpdate {

    private List<String> warningSignals;
    private List<String> copingStrategies;
    private List<SafetyPlanDto.TrustedContactDto> trustedContacts;
    private List<String> professionalContacts;
    private List<String> environmentSafetySteps;
    private String reasonForLiving;
}

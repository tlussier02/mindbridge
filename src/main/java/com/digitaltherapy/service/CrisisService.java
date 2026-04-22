package com.digitaltherapy.service;

import com.digitaltherapy.dto.*;

import java.util.List;
import java.util.UUID;

public interface CrisisService {
    CrisisHub getCrisisHub(UUID userId);
    List<CopingStrategy> getCopingStrategies();
    CrisisDetectionResultDto detectCrisis(String text);
    SafetyPlanDto getSafetyPlan(UUID userId);
    SafetyPlanDto updateSafetyPlan(UUID userId, SafetyPlanUpdate update);
}

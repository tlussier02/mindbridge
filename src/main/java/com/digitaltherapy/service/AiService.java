package com.digitaltherapy.service;

import com.digitaltherapy.dto.*;
import java.util.List;
import java.util.UUID;

public interface AiService {
    ChatResponse generateResponse(UUID sessionId, String userMessage);
    List<DistortionSuggestion> analyzeThought(String automaticThought);
    List<String> generateReframingPrompts(String thought, List<String> distortionIds);
    CrisisDetectionResultDto detectCrisis(String text);
    DiaryInsights generateInsights(UUID userId);
    SessionSummary summarizeSession(UUID sessionId);
}

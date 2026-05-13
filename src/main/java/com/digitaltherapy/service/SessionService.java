package com.digitaltherapy.service;

import com.digitaltherapy.dto.*;

import java.util.List;
import java.util.UUID;

public interface SessionService {
    List<SessionModuleDto> getSessionLibrary(UUID userId);
    SessionDetail getSessionDetails(UUID sessionId);
    ActiveSession startSession(UUID userId, UUID sessionId);
    ChatResponse chat(UUID sessionId, String message);
    SessionSummary endSession(UUID sessionId, String reason);
    List<SessionHistoryEntry> getSessionHistory(UUID userId);
}

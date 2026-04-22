package com.digitaltherapy.service;

import com.digitaltherapy.dto.*;

import java.util.List;
import java.util.UUID;

public interface ProgressService {
    WeeklyProgress getWeeklyProgress(UUID userId);
    MonthlyTrend getMonthlyTrend(UUID userId);
    BurnoutRecovery getBurnoutRecovery(UUID userId);
    List<Achievement> getAchievements(UUID userId);
}

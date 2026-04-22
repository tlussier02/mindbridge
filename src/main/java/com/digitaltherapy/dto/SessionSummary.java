package com.digitaltherapy.dto;

import java.time.LocalDateTime;
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
public class SessionSummary {

    private UUID sessionId;
    private String title;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer moodBefore;
    private Integer moodAfter;
    private String summary;
    private List<String> keyInsights;
}

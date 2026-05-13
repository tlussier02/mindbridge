package com.digitaltherapy.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionHistoryEntry {

    private UUID id;
    private String sessionTitle;
    private String moduleName;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer moodBefore;
    private Integer moodAfter;
}

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
public class DiaryEntrySummary {

    private UUID id;
    private String situation;
    private String automaticThought;
    private Integer moodBefore;
    private Integer moodAfter;
    private LocalDateTime createdAt;
    private int distortionCount;
}

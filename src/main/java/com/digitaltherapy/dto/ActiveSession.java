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
public class ActiveSession {

    private UUID sessionId;
    private UUID userSessionId;
    private String title;
    private String description;
    private LocalDateTime startedAt;
    private Integer moodBefore;
}

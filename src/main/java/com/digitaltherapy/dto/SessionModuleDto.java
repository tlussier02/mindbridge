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
public class SessionModuleDto {

    private UUID id;
    private String name;
    private String description;
    private String category;
    private Integer orderIndex;
    private List<SessionSummaryItem> sessions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionSummaryItem {

        private UUID id;
        private String title;
        private Integer durationMinutes;
    }
}

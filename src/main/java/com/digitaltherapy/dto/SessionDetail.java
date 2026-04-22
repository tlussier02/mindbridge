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
public class SessionDetail {

    private UUID id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private List<String> objectives;
    private List<String> modalities;
    private String moduleName;
}

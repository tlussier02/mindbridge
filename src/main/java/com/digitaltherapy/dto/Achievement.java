package com.digitaltherapy.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {

    private String id;
    private String name;
    private String description;
    private String icon;
    private boolean unlocked;
    private LocalDateTime unlockedAt;
    private double progress;
}

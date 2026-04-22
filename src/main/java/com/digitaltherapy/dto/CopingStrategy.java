package com.digitaltherapy.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopingStrategy {

    private String id;
    private String name;
    private String description;
    private String category;
    private List<String> steps;
    private int estimatedMinutes;
}

package com.digitaltherapy.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEntryCreate {

    @NotBlank
    private String situation;

    @NotBlank
    private String automaticThought;

    private List<EmotionRatingDto> emotions;
    private List<String> distortionIds;
    private String alternativeThought;
    private Integer moodBefore;
    private Integer moodAfter;
    private Integer beliefRatingBefore;
    private Integer beliefRatingAfter;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionRatingDto {

        private String emotion;
        private Integer intensity;
    }
}

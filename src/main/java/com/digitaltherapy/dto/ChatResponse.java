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
public class ChatResponse {

    private String message;
    private String role;
    private LocalDateTime timestamp;
    private boolean crisisDetected;
    private String crisisAction;
}

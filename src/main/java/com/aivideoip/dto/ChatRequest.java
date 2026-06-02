package com.aivideoip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for chat query requests against video transcripts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    @NotNull(message = "Video ID is required")
    private Long videoId;
    
    @NotBlank(message = "Question cannot be empty")
    private String question;
    
    @Builder.Default
    private Integer topK = 5;
    
    @Builder.Default
    private Float similarityThreshold = 0.5f;
}

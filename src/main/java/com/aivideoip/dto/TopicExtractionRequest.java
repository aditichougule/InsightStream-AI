package com.aivideoip.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for topic extraction from transcript
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicExtractionRequest {
    
    @NotNull(message = "Video ID is required")
    private Long videoId;
    
    private String transcript;
    
    private Boolean includeDescriptions;
}

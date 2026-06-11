package com.aivideoip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for semantic search queries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchRequest {
    
    @NotBlank(message = "Search query cannot be empty")
    private String query;
    
    private Long videoId;  // Optional: search within specific video or null for global search
    
    @Builder.Default
    private Integer topK = 10;
    
    @Builder.Default
    private Float similarityThreshold = 0.3f;
    
    @Builder.Default
    private Boolean includeMetadata = true;
}

package com.aivideoip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for importing videos from YouTube.
 * Handles YouTube URL validation and optional custom metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeImportRequest {
    
    @NotBlank(message = "YouTube URL is required")
    private String youtubeUrl;
    
    private String title;
    
    private String description;
    
    @NotBlank(message = "User ID is required")
    private String userId;
}

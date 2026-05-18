package com.aivideoip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for uploading video files.
 * Captures metadata about the video being uploaded.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadRequest {
    
    @NotBlank(message = "Video title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "User ID is required")
    private Long userId;
}

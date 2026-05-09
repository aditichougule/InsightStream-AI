package com.aivideoip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for video responses
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoDTO {

    private Long id;
    private String title;
    private String description;
    private String sourceUrl;
    private String source;
    private String thumbnailUrl;
    private Long durationSeconds;
    private Long ownerId;
    private String processingStatus;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

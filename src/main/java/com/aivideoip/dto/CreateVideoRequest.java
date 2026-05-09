package com.aivideoip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a video
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateVideoRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Source URL is required")
    private String sourceUrl;

    @NotNull(message = "Video source is required")
    private String source; // YOUTUBE, UPLOADED, URL, PODCAST, WEBINAR

    private String thumbnailUrl;
    private Long durationSeconds;
}

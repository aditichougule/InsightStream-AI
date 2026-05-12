package com.aivideoip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryDTO {
    
    private Long id;
    private Long videoId;
    private String summaryText;
    private String keyPoints;
    private String summaryType;
}

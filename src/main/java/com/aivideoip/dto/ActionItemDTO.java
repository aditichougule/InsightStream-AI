package com.aivideoip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ActionItem
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionItemDTO {
    
    private Long id;
    private Long videoId;
    private String title;
    private String description;
    private String assignedTo;
    private String status;
    private Integer timeReference;
    private String priority;
}

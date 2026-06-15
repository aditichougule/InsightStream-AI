package com.aivideoip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Request DTO for knowledge base indexing operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KBIndexRequest {
    
    @NotBlank(message = "Knowledge base name cannot be empty")
    private String kbName;
    
    @NotNull(message = "Video IDs cannot be null")
    private Set<Long> videoIds;
    
    @Builder.Default
    private Boolean reindex = false;  // If true, replace existing index
    
    @Builder.Default
    private String description = "";
}

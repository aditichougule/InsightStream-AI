package com.aivideoip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for knowledge base status and information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KBStatusDTO {
    
    @JsonProperty("kbName")
    private String kbName;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("videoCount")
    private Integer videoCount;
    
    @JsonProperty("chunkCount")
    private Long chunkCount;
    
    @JsonProperty("indexed")
    private Boolean indexed;
    
    @JsonProperty("createdAt")
    private Long createdAt;
    
    @JsonProperty("lastUpdatedAt")
    private Long lastUpdatedAt;
    
    @JsonProperty("videos")
    private List<KBVideoInfo> videos;
    
    @JsonProperty("status")
    private String status;  // ACTIVE, INDEXING, FAILED
    
    @JsonProperty("progress")
    private Integer progress;  // 0-100
    
    /**
     * Information about a video in the knowledge base
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KBVideoInfo {
        private Long videoId;
        private String title;
        private Integer chunkCount;
        private Boolean embedded;
    }
}

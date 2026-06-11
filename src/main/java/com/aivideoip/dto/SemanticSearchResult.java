package com.aivideoip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for semantic search results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchResult {
    
    @JsonProperty("query")
    private String query;
    
    @JsonProperty("videoId")
    private Long videoId;
    
    @JsonProperty("matches")
    private List<SearchMatch> matches;
    
    @JsonProperty("totalMatches")
    private Integer totalMatches;
    
    @JsonProperty("processingTimeMs")
    private Long processingTimeMs;
    
    /**
     * Individual search match with context and metadata
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMatch {
        
        @JsonProperty("chunkId")
        private Long chunkId;
        
        @JsonProperty("videoTitle")
        private String videoTitle;
        
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("startSeconds")
        private Integer startSeconds;
        
        @JsonProperty("endSeconds")
        private Integer endSeconds;
        
        @JsonProperty("startTime")
        private String startTime;
        
        @JsonProperty("endTime")
        private String endTime;
        
        @JsonProperty("similarityScore")
        private Float similarityScore;
        
        @JsonProperty("context")
        private String context;  // Surrounding context for better understanding
        
        @JsonProperty("rank")
        private Integer rank;
    }
}

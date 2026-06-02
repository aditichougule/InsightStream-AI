package com.aivideoip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for chat query responses with source citations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    
    @JsonProperty("answer")
    private String answer;
    
    @JsonProperty("sources")
    private List<ChatSource> sources;
    
    @JsonProperty("confidence")
    private Float confidence;
    
    @JsonProperty("generatedAt")
    private Long generatedAt;
    
    @JsonProperty("processingTimeMs")
    private Long processingTimeMs;
    
    /**
     * Source chunk information with timestamps and similarity
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatSource {
        private Long chunkId;
        private String text;
        private Integer startSeconds;
        private Integer endSeconds;
        private String startTime;
        private String endTime;
        private Float similarityScore;
    }
}

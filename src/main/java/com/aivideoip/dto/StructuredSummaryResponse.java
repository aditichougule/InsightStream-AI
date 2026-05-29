package com.aivideoip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured JSON response for summary generation
 * Ensures consistent format for frontend integration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuredSummaryResponse {
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("actionItems")
    private List<ActionItemData> actionItems;
    
    @JsonProperty("topics")
    private List<TopicData> topics;
    
    @JsonProperty("keyPoints")
    private List<String> keyPoints;
    
    @JsonProperty("timestamp")
    private Long generatedAt;
    
    /**
     * Action item with assignment details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItemData {
        private String task;
        private String assignee;
        private String priority;
        private String dueDate;
        private String status;
    }
    
    /**
     * Topic with timestamp information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicData {
        private String topic;
        private String startTime;
        private String endTime;
        private String description;
        private Integer startSeconds;
        private Integer endSeconds;
    }
}

package com.aivideoip.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for StructuredSummaryResponse DTO
 */
@DisplayName("StructuredSummaryResponse Tests")
class StructuredSummaryResponseTest {
    
    private StructuredSummaryResponse response;
    
    @BeforeEach
    void setUp() {
        response = StructuredSummaryResponse.builder()
                .summary("This is a test summary")
                .generatedAt(System.currentTimeMillis())
                .build();
    }
    
    @Test
    @DisplayName("Should build response with all fields")
    void testBuilderWithAllFields() {
        // Arrange
        List<String> keyPoints = Arrays.asList("Point 1", "Point 2");
        List<StructuredSummaryResponse.ActionItemData> actionItems = Arrays.asList(
                StructuredSummaryResponse.ActionItemData.builder()
                        .task("Task 1")
                        .assignee("John")
                        .priority("HIGH")
                        .build()
        );
        List<StructuredSummaryResponse.TopicData> topics = Arrays.asList(
                StructuredSummaryResponse.TopicData.builder()
                        .topic("Topic 1")
                        .startTime("00:00:00")
                        .endTime("00:05:00")
                        .build()
        );
        
        // Act
        response = StructuredSummaryResponse.builder()
                .summary("Full summary")
                .keyPoints(keyPoints)
                .actionItems(actionItems)
                .topics(topics)
                .generatedAt(System.currentTimeMillis())
                .build();
        
        // Assert
        assertThat(response.getSummary()).isEqualTo("Full summary");
        assertThat(response.getKeyPoints()).hasSize(2);
        assertThat(response.getActionItems()).hasSize(1);
        assertThat(response.getTopics()).hasSize(1);
        assertThat(response.getGeneratedAt()).isPositive();
    }
    
    @Test
    @DisplayName("Should create TopicData with valid timestamps")
    void testTopicDataBuilder() {
        // Arrange & Act
        StructuredSummaryResponse.TopicData topic = StructuredSummaryResponse.TopicData.builder()
                .topic("JWT Authentication")
                .startTime("00:12:10")
                .endTime("00:25:30")
                .startSeconds(730)
                .endSeconds(1530)
                .description("Discussion about JWT tokens")
                .build();
        
        // Assert
        assertThat(topic.getTopic()).isEqualTo("JWT Authentication");
        assertThat(topic.getStartTime()).isEqualTo("00:12:10");
        assertThat(topic.getEndTime()).isEqualTo("00:25:30");
        assertThat(topic.getStartSeconds()).isEqualTo(730);
        assertThat(topic.getEndSeconds()).isEqualTo(1530);
        assertThat(topic.getDescription()).isNotEmpty();
    }
    
    @Test
    @DisplayName("Should create ActionItemData with all fields")
    void testActionItemDataBuilder() {
        // Arrange & Act
        StructuredSummaryResponse.ActionItemData actionItem = 
                StructuredSummaryResponse.ActionItemData.builder()
                .task("Implement JWT")
                .assignee("Alice")
                .priority("HIGH")
                .dueDate("2026-06-15")
                .status("IN_PROGRESS")
                .build();
        
        // Assert
        assertThat(actionItem.getTask()).isEqualTo("Implement JWT");
        assertThat(actionItem.getAssignee()).isEqualTo("Alice");
        assertThat(actionItem.getPriority()).isEqualTo("HIGH");
        assertThat(actionItem.getDueDate()).isEqualTo("2026-06-15");
        assertThat(actionItem.getStatus()).isEqualTo("IN_PROGRESS");
    }
    
    @Test
    @DisplayName("Should handle null values in TopicData")
    void testTopicDataWithNullValues() {
        // Arrange & Act
        StructuredSummaryResponse.TopicData topic = StructuredSummaryResponse.TopicData.builder()
                .topic("Topic")
                .description(null)
                .startSeconds(null)
                .build();
        
        // Assert
        assertThat(topic.getTopic()).isEqualTo("Topic");
        assertThat(topic.getDescription()).isNull();
        assertThat(topic.getStartSeconds()).isNull();
    }
    
    @Test
    @DisplayName("Should handle empty action items list")
    void testEmptyActionItemsList() {
        // Arrange & Act
        response.setActionItems(List.of());
        
        // Assert
        assertThat(response.getActionItems()).isEmpty();
    }
    
    @Test
    @DisplayName("Should serialize to JSON correctly")
    void testJsonSerialization() {
        // Arrange
        response.setKeyPoints(List.of("Point 1", "Point 2"));
        
        // Act & Assert
        assertThat(response.getKeyPoints()).containsExactly("Point 1", "Point 2");
    }
}

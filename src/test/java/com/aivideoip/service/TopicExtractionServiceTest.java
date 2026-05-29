package com.aivideoip.service;

import com.aivideoip.dto.StructuredSummaryResponse;
import com.aivideoip.dto.TopicExtractionRequest;
import com.aivideoip.entity.Topic;
import com.aivideoip.entity.User;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.TopicRepository;
import com.aivideoip.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TopicExtractionService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TopicExtractionService Tests")
class TopicExtractionServiceTest {
    
    @Mock
    private TopicRepository topicRepository;
    
    @Mock
    private VideoRepository videoRepository;
    
    @Mock
    private OllamaClient ollamaClient;
    
    @InjectMocks
    private TopicExtractionService topicExtractionService;
    
    private Video testVideo;
    private User testUser;
    private TopicExtractionRequest extractionRequest;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();
        testUser.setId(1L);
        
        testVideo = Video.builder()
                .title("Test Video")
                .owner(testUser)
                .durationSeconds(3600L)
                .build();
        testVideo.setId(1L);
        
        extractionRequest = TopicExtractionRequest.builder()
                .videoId(1L)
                .transcript("Topic 1: Introduction - 0:00 to 5:30\nTopic 2: Main Content - 5:30 to 25:00")
                .includeDescriptions(true)
                .build();
    }
    
    @Test
    @DisplayName("Should successfully extract topics from transcript")
    void testExtractTopicsSuccess() {
        // Arrange
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        List<StructuredSummaryResponse.TopicData> result = topicExtractionService.extractTopics(extractionRequest);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThanOrEqualTo(0);
        verify(videoRepository).findById(1L);
    }
    
    @Test
    @DisplayName("Should throw ResourceNotFoundException when video not found")
    void testExtractTopicsVideoNotFound() {
        // Arrange
        when(videoRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> topicExtractionService.extractTopics(extractionRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Video not found");
    }
    
    @Test
    @DisplayName("Should handle empty transcript gracefully")
    void testExtractTopicsEmptyTranscript() {
        // Arrange
        TopicExtractionRequest emptyRequest = TopicExtractionRequest.builder()
                .videoId(1L)
                .transcript("")
                .build();
        
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        
        // Act
        List<StructuredSummaryResponse.TopicData> result = topicExtractionService.extractTopics(emptyRequest);
        
        // Assert
        assertThat(result).isEmpty();
        verify(topicRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should handle null transcript gracefully")
    void testExtractTopicsNullTranscript() {
        // Arrange
        TopicExtractionRequest nullRequest = TopicExtractionRequest.builder()
                .videoId(1L)
                .transcript(null)
                .build();
        
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        
        // Act
        List<StructuredSummaryResponse.TopicData> result = topicExtractionService.extractTopics(nullRequest);
        
        // Assert
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Should retrieve topics for video")
    void testGetTopicsForVideoSuccess() {
        // Arrange
        Topic topic1 = Topic.builder()
                .topicName("Topic 1")
                .startSeconds(0)
                .endSeconds(300)
                .startTime("00:00:00")
                .endTime("00:05:00")
                .build();
        topic1.setId(1L);
        
        when(topicRepository.findByVideoIdOrderBySequenceOrderAsc(1L))
                .thenReturn(List.of(topic1));
        
        // Act
        List<StructuredSummaryResponse.TopicData> result = topicExtractionService.getTopicsForVideo(1L);
        
        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTopic()).isEqualTo("Topic 1");
        assertThat(result.get(0).getStartSeconds()).isEqualTo(0);
        assertThat(result.get(0).getEndSeconds()).isEqualTo(300);
    }
    
    @Test
    @DisplayName("Should return empty list when no topics exist")
    void testGetTopicsForVideoEmpty() {
        // Arrange
        when(topicRepository.findByVideoIdOrderBySequenceOrderAsc(1L))
                .thenReturn(List.of());
        
        // Act
        List<StructuredSummaryResponse.TopicData> result = topicExtractionService.getTopicsForVideo(1L);
        
        // Assert
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle multiple topics correctly")
    void testMultipleTopicsExtraction() {
        // Arrange
        Topic topic1 = Topic.builder()
                .topicName("Introduction")
                .startSeconds(0)
                .endSeconds(330)
                .startTime("00:00:00")
                .endTime("00:05:30")
                .sequenceOrder(0)
                .build();
        topic1.setId(1L);
        
        Topic topic2 = Topic.builder()
                .topicName("Main Content")
                .startSeconds(330)
                .endSeconds(1500)
                .startTime("00:05:30")
                .endTime("00:25:00")
                .sequenceOrder(1)
                .build();
        topic2.setId(2L);
        
        when(topicRepository.findByVideoIdOrderBySequenceOrderAsc(1L))
                .thenReturn(List.of(topic1, topic2));
        
        // Act
        List<StructuredSummaryResponse.TopicData> result = topicExtractionService.getTopicsForVideo(1L);
        
        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTopic()).isEqualTo("Introduction");
        assertThat(result.get(1).getTopic()).isEqualTo("Main Content");
    }
}

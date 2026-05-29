package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.StructuredSummaryResponse;
import com.aivideoip.dto.TopicExtractionRequest;
import com.aivideoip.service.TopicExtractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TopicExtractionController
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TopicExtractionController Tests")
class TopicExtractionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TopicExtractionService topicExtractionService;
    
    private TopicExtractionRequest testRequest;
    private List<StructuredSummaryResponse.TopicData> testTopics;
    
    @BeforeEach
    void setUp() {
        testRequest = TopicExtractionRequest.builder()
                .videoId(1L)
                .transcript("Topic 1: Introduction\nTopic 2: Main Content")
                .includeDescriptions(true)
                .build();
        
        testTopics = List.of(
                StructuredSummaryResponse.TopicData.builder()
                        .topic("Introduction")
                        .startTime("00:00:00")
                        .endTime("00:05:30")
                        .startSeconds(0)
                        .endSeconds(330)
                        .description("Introduction to the topic")
                        .build(),
                StructuredSummaryResponse.TopicData.builder()
                        .topic("Main Content")
                        .startTime("00:05:30")
                        .endTime("00:25:00")
                        .startSeconds(330)
                        .endSeconds(1500)
                        .description("Main content discussion")
                        .build()
        );
    }
    
    @Test
    @DisplayName("Should extract topics successfully")
    void testExtractTopicsSuccess() throws Exception {
        // Arrange
        when(topicExtractionService.extractTopics(org.mockito.ArgumentMatchers.any(TopicExtractionRequest.class)))
                .thenReturn(testTopics);
        
        // Act & Assert
        mockMvc.perform(post("/api/topics/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.length()", is(2)))
                .andExpect(jsonPath("$.data[0].topic", is("Introduction")))
                .andExpect(jsonPath("$.data[0].startTime", is("00:00:00")))
                .andExpect(jsonPath("$.data[1].topic", is("Main Content")))
                .andExpect(jsonPath("$.message", containsString("successfully")));
        
        verify(topicExtractionService, times(1)).extractTopics(org.mockito.ArgumentMatchers.any(TopicExtractionRequest.class));
    }
    
    @Test
    @DisplayName("Should retrieve topics for video")
    void testGetTopicsForVideoSuccess() throws Exception {
        // Arrange
        when(topicExtractionService.getTopicsForVideo(1L))
                .thenReturn(testTopics);
        
        // Act & Assert
        mockMvc.perform(get("/api/topics/video/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.length()", is(2)))
                .andExpect(jsonPath("$.data[0].topic", is("Introduction")))
                .andExpect(jsonPath("$.data[0].startSeconds", is(0)))
                .andExpect(jsonPath("$.data[1].topic", is("Main Content")))
                .andExpect(jsonPath("$.data[1].startSeconds", is(330)));
        
        verify(topicExtractionService, times(1)).getTopicsForVideo(1L);
    }
    
    @Test
    @DisplayName("Should return empty list when no topics exist")
    void testGetTopicsForVideoEmpty() throws Exception {
        // Arrange
        when(topicExtractionService.getTopicsForVideo(1L))
                .thenReturn(List.of());
        
        // Act & Assert
        mockMvc.perform(get("/api/topics/video/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.length()", is(0)));
        
        verify(topicExtractionService, times(1)).getTopicsForVideo(1L);
    }
    
    @Test
    @DisplayName("Should handle invalid video ID parameter")
    void testGetTopicsForVideoInvalidId() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/topics/video/invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should validate required fields in extraction request")
    void testExtractTopicsValidation() throws Exception {
        // Arrange
        TopicExtractionRequest invalidRequest = TopicExtractionRequest.builder()
                .videoId(null)  // Missing required field
                .transcript("Some transcript")
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/topics/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should return proper API response structure")
    void testResponseStructure() throws Exception {
        // Arrange
        when(topicExtractionService.extractTopics(org.mockito.ArgumentMatchers.any(TopicExtractionRequest.class)))
                .thenReturn(testTopics);
        
        // Act
        MvcResult result = mockMvc.perform(post("/api/topics/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        
        // Assert
        String response = result.getResponse().getContentAsString();
        ApiResponse<?> apiResponse = objectMapper.readValue(response, ApiResponse.class);
        assertThat(apiResponse.isSuccess()).isTrue();
        assertThat(apiResponse.getStatusCode()).isEqualTo(201);
        assertThat(apiResponse.getMessage()).isNotEmpty();
    }
    
    @Test
    @DisplayName("Should include topic timestamps in response")
    void testTopicTimestampsInResponse() throws Exception {
        // Arrange
        when(topicExtractionService.extractTopics(org.mockito.ArgumentMatchers.any(TopicExtractionRequest.class)))
                .thenReturn(testTopics);
        
        // Act & Assert
        mockMvc.perform(post("/api/topics/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].startTime", notNullValue()))
                .andExpect(jsonPath("$.data[0].endTime", notNullValue()))
                .andExpect(jsonPath("$.data[0].startSeconds", notNullValue()))
                .andExpect(jsonPath("$.data[0].endSeconds", notNullValue()));
    }
}

package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.SummaryDTO;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.service.SummaryGenerationService;
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
import org.springframework.test.web.servlet.ResultActions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SummaryGenerationController
 * Tests REST API endpoints for summary generation
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SummaryGenerationController Tests")
class SummaryGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SummaryGenerationService summaryGenerationService;

    private SummaryDTO testSummaryDTO;
    private List<Map<String, Object>> testActionItems;
    private List<Map<String, Object>> testConcepts;

    @BeforeEach
    void setUp() {
        // Create test summary DTO
        testSummaryDTO = SummaryDTO.builder()
                .id(1L)
                .videoId(1L)
                .summaryText("Comprehensive test summary")
                .keyPoints("Point 1, Point 2, Point 3")
                .summaryType("COMPREHENSIVE")
                .metadata("{\"notes\": [\"Note 1\", \"Note 2\"]}")
                .build();

        // Create test action items
        testActionItems = new ArrayList<>();
        Map<String, Object> actionItem1 = new HashMap<>();
        actionItem1.put("action", "Review chapter 2");
        actionItem1.put("timestamp", "00:05:30");
        actionItem1.put("owner", "Team");
        actionItem1.put("priority", "High");
        testActionItems.add(actionItem1);

        Map<String, Object> actionItem2 = new HashMap<>();
        actionItem2.put("action", "Submit assignment");
        actionItem2.put("timestamp", "00:15:45");
        actionItem2.put("owner", "Student");
        actionItem2.put("priority", "Medium");
        testActionItems.add(actionItem2);

        // Create test concepts
        testConcepts = new ArrayList<>();
        Map<String, Object> concept1 = new HashMap<>();
        concept1.put("concept", "Machine Learning");
        concept1.put("explanation", "The field of AI focused on algorithms");
        concept1.put("importance", 9.5);
        testConcepts.add(concept1);

        Map<String, Object> concept2 = new HashMap<>();
        concept2.put("concept", "Neural Networks");
        concept2.put("explanation", "Computational models inspired by brain");
        concept2.put("importance", 8.7);
        testConcepts.add(concept2);
    }

    @Test
    @DisplayName("Should generate comprehensive summary successfully")
    void testGenerateComprehensiveSummary_Success() throws Exception {
        // Given
        when(summaryGenerationService.generateComprehensiveSummary(1L))
                .thenReturn(testSummaryDTO);

        // When
        ResultActions result = mockMvc.perform(
                post("/api/summary/generation/comprehensive/1")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.videoId", is(1)))
                .andExpect(jsonPath("$.data.summaryText", containsString("Comprehensive")))
                .andExpect(jsonPath("$.data.summaryType", is("COMPREHENSIVE")));

        verify(summaryGenerationService, times(1)).generateComprehensiveSummary(1L);
    }

    @Test
    @DisplayName("Should handle video not found in comprehensive summary")
    void testGenerateComprehensiveSummary_VideoNotFound() throws Exception {
        // Given
        when(summaryGenerationService.generateComprehensiveSummary(1L))
                .thenThrow(new ResourceNotFoundException("Video not found"));

        // When & Then
        mockMvc.perform(
                post("/api/summary/generation/comprehensive/1")
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isNotFound());

        verify(summaryGenerationService, times(1)).generateComprehensiveSummary(1L);
    }

    @Test
    @DisplayName("Should generate async comprehensive summary successfully")
    void testGenerateComprehensiveSummaryAsync_Success() throws Exception {
        // Given
        when(summaryGenerationService.generateComprehensiveSummary(1L))
                .thenReturn(testSummaryDTO);

        // When
        ResultActions result = mockMvc.perform(
                post("/api/summary/generation/comprehensive/1/async")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("processing")));

        verify(summaryGenerationService, times(1)).generateComprehensiveSummary(1L);
    }

    @Test
    @DisplayName("Should generate BRIEF summary successfully")
    void testGenerateSummaryByType_Brief() throws Exception {
        // Given
        SummaryDTO briefDTO = SummaryDTO.builder()
                .id(1L)
                .videoId(1L)
                .summaryText("Brief summary")
                .summaryType("BRIEF")
                .build();

        when(summaryGenerationService.generateSummaryByType(1L, "BRIEF"))
                .thenReturn(briefDTO);

        // When
        ResultActions result = mockMvc.perform(
                post("/api/summary/generation/BRIEF/1")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.summaryType", is("BRIEF")))
                .andExpect(jsonPath("$.data.summaryText", is("Brief summary")));

        verify(summaryGenerationService, times(1)).generateSummaryByType(1L, "BRIEF");
    }

    @Test
    @DisplayName("Should generate DETAILED summary successfully")
    void testGenerateSummaryByType_Detailed() throws Exception {
        // Given
        SummaryDTO detailedDTO = SummaryDTO.builder()
                .id(1L)
                .videoId(1L)
                .summaryText("Detailed summary with comprehensive information")
                .summaryType("DETAILED")
                .build();

        when(summaryGenerationService.generateSummaryByType(1L, "DETAILED"))
                .thenReturn(detailedDTO);

        // When
        ResultActions result = mockMvc.perform(
                post("/api/summary/generation/DETAILED/1")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.summaryType", is("DETAILED")));

        verify(summaryGenerationService, times(1)).generateSummaryByType(1L, "DETAILED");
    }

    @Test
    @DisplayName("Should return error for invalid summary type")
    void testGenerateSummaryByType_InvalidType() throws Exception {
        // Given
        when(summaryGenerationService.generateSummaryByType(1L, "INVALID"))
                .thenThrow(new IllegalArgumentException("Invalid summary type"));

        // When & Then
        mockMvc.perform(
                post("/api/summary/generation/INVALID/1")
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should extract action items with timestamps")
    void testExtractActionItems_Success() throws Exception {
        // Given
        when(summaryGenerationService.extractActionItemsWithTimestamps(1L))
                .thenReturn(testActionItems);

        // When
        ResultActions result = mockMvc.perform(
                post("/api/summary/generation/action-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", isA(List.class)))
                .andExpect(jsonPath("$.data.length()", is(2)))
                .andExpect(jsonPath("$.data[0].action", is("Review chapter 2")))
                .andExpect(jsonPath("$.data[0].timestamp", is("00:05:30")))
                .andExpect(jsonPath("$.data[0].priority", is("High")))
                .andExpect(jsonPath("$.data[1].action", is("Submit assignment")))
                .andExpect(jsonPath("$.data[1].priority", is("Medium")));

        verify(summaryGenerationService, times(1)).extractActionItemsWithTimestamps(1L);
    }

    @Test
    @DisplayName("Should handle no action items found")
    void testExtractActionItems_Empty() throws Exception {
        // Given
        when(summaryGenerationService.extractActionItemsWithTimestamps(1L))
                .thenReturn(new ArrayList<>());

        // When
        ResultActions result = mockMvc.perform(
                post("/api/summary/generation/action-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.length()", is(0)));

        verify(summaryGenerationService, times(1)).extractActionItemsWithTimestamps(1L);
    }

    @Test
    @DisplayName("Should extract key concepts with importance scores")
    void testExtractKeyConcepts_Success() throws Exception {
        // Given
        when(summaryGenerationService.extractKeyConcepts(1L))
                .thenReturn(testConcepts);

        // When
        ResultActions result = mockMvc.perform(
                post("/api/summary/generation/key-concepts/1")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", isA(List.class)))
                .andExpect(jsonPath("$.data.length()", is(2)))
                .andExpect(jsonPath("$.data[0].concept", is("Machine Learning")))
                .andExpect(jsonPath("$.data[0].importance", is(9.5)))
                .andExpect(jsonPath("$.data[1].concept", is("Neural Networks")))
                .andExpect(jsonPath("$.data[1].importance", is(8.7)));

        verify(summaryGenerationService, times(1)).extractKeyConcepts(1L);
    }

    @Test
    @DisplayName("Should return error when video not found for action items")
    void testExtractActionItems_VideoNotFound() throws Exception {
        // Given
        when(summaryGenerationService.extractActionItemsWithTimestamps(999L))
                .thenThrow(new ResourceNotFoundException("Video not found"));

        // When & Then
        mockMvc.perform(
                post("/api/summary/generation/action-items/999")
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isNotFound());

        verify(summaryGenerationService, times(1)).extractActionItemsWithTimestamps(999L);
    }

    @Test
    @DisplayName("Should return error when video not found for key concepts")
    void testExtractKeyConcepts_VideoNotFound() throws Exception {
        // Given
        when(summaryGenerationService.extractKeyConcepts(999L))
                .thenThrow(new ResourceNotFoundException("Video not found"));

        // When & Then
        mockMvc.perform(
                post("/api/summary/generation/key-concepts/999")
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isNotFound());

        verify(summaryGenerationService, times(1)).extractKeyConcepts(999L);
    }

    @Test
    @DisplayName("Should handle server error gracefully")
    void testGenerateComprehensiveSummary_ServerError() throws Exception {
        // Given
        when(summaryGenerationService.generateComprehensiveSummary(1L))
                .thenThrow(new RuntimeException("LLM service unavailable"));

        // When & Then
        mockMvc.perform(
                post("/api/summary/generation/comprehensive/1")
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should return proper response structure")
    void testResponseStructure() throws Exception {
        // Given
        when(summaryGenerationService.generateComprehensiveSummary(1L))
                .thenReturn(testSummaryDTO);

        // When
        MvcResult result = mockMvc.perform(
                post("/api/summary/generation/comprehensive/1")
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String content = result.getResponse().getContentAsString();
        ApiResponse<?> apiResponse = objectMapper.readValue(content, ApiResponse.class);
        assertEquals(true, apiResponse.getSuccess());
        assertEquals(200, apiResponse.getStatusCode());
    }

    @Test
    @DisplayName("Should handle multiple concurrent requests")
    void testConcurrentRequests() throws Exception {
        // Given
        when(summaryGenerationService.generateComprehensiveSummary(anyLong()))
                .thenReturn(testSummaryDTO);

        // When - Simulate multiple requests
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(
                    post("/api/summary/generation/comprehensive/" + i)
                            .contentType(MediaType.APPLICATION_JSON)
            )
                    .andExpect(status().isOk());
        }

        // Then
        verify(summaryGenerationService, times(3)).generateComprehensiveSummary(anyLong());
    }

    @Test
    @DisplayName("Should validate video ID parameter")
    void testInvalidVideoIdParameter() throws Exception {
        // When & Then - Invalid video ID should be rejected
        mockMvc.perform(
                post("/api/summary/generation/comprehensive/invalid-id")
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isBadRequest());
    }
}

package com.aivideoip.integration;

import com.aivideoip.dto.SemanticSearchRequest;
import com.aivideoip.service.SemanticSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SemanticSearchIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private SemanticSearchService semanticSearchService;
    
    private SemanticSearchRequest validRequest;
    
    @BeforeEach
    void setUp() {
        validRequest = SemanticSearchRequest.builder()
                .query("deployment discussion")
                .videoId(1L)
                .topK(10)
                .similarityThreshold(0.3f)
                .build();
    }
    
    @Test
    void testSearchEndpointWithValidRequest() throws Exception {
        mockMvc.perform(post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)));
    }
    
    @Test
    void testSearchEndpointWithEmptyQuery() throws Exception {
        SemanticSearchRequest request = SemanticSearchRequest.builder()
                .query("")
                .videoId(1L)
                .build();
        
        mockMvc.perform(post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testSearchEndpointWithNullQuery() throws Exception {
        SemanticSearchRequest request = SemanticSearchRequest.builder()
                .videoId(1L)
                .build();
        
        mockMvc.perform(post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGlobalSearchWithoutVideoId() throws Exception {
        SemanticSearchRequest request = SemanticSearchRequest.builder()
                .query("deployment discussion")
                .topK(10)
                .build();
        
        mockMvc.perform(post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
    
    @Test
    void testSearchResponseStructure() throws Exception {
        mockMvc.perform(post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.query", is("deployment discussion")))
                .andExpect(jsonPath("$.data.matches", isA(Object.class)))
                .andExpect(jsonPath("$.data.totalMatches", isA(Integer.class)))
                .andExpect(jsonPath("$.data.processingTimeMs", isA(Number.class)));
    }
    
    @Test
    void testHealthCheckEndpoint() throws Exception {
        mockMvc.perform(get("/api/search/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("healthy")))
                .andExpect(jsonPath("$.data", is("OK")));
    }
    
    @Test
    void testSearchWithCustomParameters() throws Exception {
        SemanticSearchRequest request = SemanticSearchRequest.builder()
                .query("machine learning deployment")
                .topK(20)
                .similarityThreshold(0.5f)
                .build();
        
        mockMvc.perform(post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}

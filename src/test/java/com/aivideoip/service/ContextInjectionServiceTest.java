package com.aivideoip.service;

import com.aivideoip.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ContextInjectionServiceTest {
    
    @InjectMocks
    private ContextInjectionService contextInjectionService;
    
    private List<ChatResponse.ChatSource> testSources;
    
    @BeforeEach
    void setUp() {
        ChatResponse.ChatSource source1 = ChatResponse.ChatSource.builder()
                .chunkId(1L)
                .text("This is the first important point in the video.")
                .startSeconds(0)
                .endSeconds(10)
                .startTime("00:00:00")
                .endTime("00:00:10")
                .similarityScore(0.85f)
                .build();
        
        ChatResponse.ChatSource source2 = ChatResponse.ChatSource.builder()
                .chunkId(2L)
                .text("And here is the second related point.")
                .startSeconds(20)
                .endSeconds(35)
                .startTime("00:00:20")
                .endTime("00:00:35")
                .similarityScore(0.78f)
                .build();
        
        testSources = Arrays.asList(source1, source2);
    }
    
    @Test
    void testBuildContextInjectedPrompt_WithContext() {
        String context = "Test context";
        String question = "What is the main point?";
        String videoTitle = "Test Video";
        
        String prompt = contextInjectionService.buildContextInjectedPrompt(
                question, context, videoTitle, true);
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("CRITICAL INSTRUCTIONS"));
        assertTrue(prompt.contains("Answer ONLY using the transcript context"));
        assertTrue(prompt.contains("Not found in video."));
        assertTrue(prompt.contains(question));
    }
    
    @Test
    void testBuildContextInjectedPrompt_NoContext() {
        String question = "What is the main point?";
        String videoTitle = "Test Video";
        
        String prompt = contextInjectionService.buildContextInjectedPrompt(
                question, "", videoTitle, false);
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("no relevant transcript context was found"));
    }
    
    @Test
    void testFormatContextWithBoundaries() {
        String formatted = contextInjectionService.formatContextWithBoundaries(testSources);
        
        assertNotNull(formatted);
        assertTrue(formatted.contains("[CHUNK 1]"));
        assertTrue(formatted.contains("[CHUNK 2]"));
        assertTrue(formatted.contains("[00:00:00 - 00:00:10]"));
        assertTrue(formatted.contains("This is the first important point"));
    }
    
    @Test
    void testFormatContextWithBoundaries_EmptyList() {
        String formatted = contextInjectionService.formatContextWithBoundaries(Arrays.asList());
        
        assertNotNull(formatted);
        assertTrue(formatted.isEmpty());
    }
    
    @Test
    void testBuildSystemPrompt() {
        String systemPrompt = contextInjectionService.buildSystemPrompt();
        
        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("ONLY uses provided context"));
        assertTrue(systemPrompt.contains("Not found in video."));
    }
    
    @Test
    void testEvaluateContextQuality_HighQuality() {
        ContextInjectionService.ContextQualityMetrics metrics = 
                contextInjectionService.evaluateContextQuality(testSources);
        
        assertNotNull(metrics);
        assertTrue(metrics.isHasContext());
        assertEquals(2, metrics.getChunkCount());
        assertTrue(metrics.getAverageSimilarity() > 0.7f);
        assertTrue(metrics.isHighQuality());
    }
    
    @Test
    void testEvaluateContextQuality_EmptyList() {
        ContextInjectionService.ContextQualityMetrics metrics = 
                contextInjectionService.evaluateContextQuality(Arrays.asList());
        
        assertNotNull(metrics);
        assertFalse(metrics.isHasContext());
        assertEquals(0, metrics.getChunkCount());
        assertFalse(metrics.isHighQuality());
    }
    
    @Test
    void testSanitizeResponse_ValidResponse() {
        String response = "The main point is very important.";
        
        String sanitized = contextInjectionService.sanitizeResponse(response, testSources);
        
        assertEquals(response, sanitized);
    }
    
    @Test
    void testSanitizeResponse_NotFoundVariants() {
        String[] notFoundVariants = {
            "Not found in video.",
            "I don't know.",
            "Not found.",
            "This information is not provided in the context"
        };
        
        for (String variant : notFoundVariants) {
            String sanitized = contextInjectionService.sanitizeResponse(variant, testSources);
            
            if (!variant.equalsIgnoreCase("Not found in video.")) {
                assertEquals("Not found in video.", sanitized);
            }
        }
    }
    
    @Test
    void testSanitizeResponse_NullResponse() {
        String sanitized = contextInjectionService.sanitizeResponse(null, testSources);
        
        assertEquals("Not found in video.", sanitized);
    }
    
    @Test
    void testSanitizeResponse_EmptyResponse() {
        String sanitized = contextInjectionService.sanitizeResponse("   ", testSources);
        
        assertEquals("Not found in video.", sanitized);
    }
    
    @Test
    void testSanitizeResponse_ContextLimitationPhrase() {
        String response = "This information is not mentioned in the provided context.";
        
        String sanitized = contextInjectionService.sanitizeResponse(response, testSources);
        
        assertEquals("Not found in video.", sanitized);
    }
}

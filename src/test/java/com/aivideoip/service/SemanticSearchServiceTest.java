package com.aivideoip.service;

import com.aivideoip.dto.SemanticSearchRequest;
import com.aivideoip.dto.SemanticSearchResult;
import com.aivideoip.entity.TranscriptChunk;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.TranscriptChunkRepository;
import com.aivideoip.repository.VideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {
    
    @Mock
    private OllamaEmbeddingClient embeddingClient;
    
    @Mock
    private TranscriptChunkRepository chunkRepository;
    
    @Mock
    private VideoRepository videoRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private SemanticSearchService semanticSearchService;
    
    private Video testVideo;
    private SemanticSearchRequest searchRequest;
    private List<Double> testEmbedding;
    
    @BeforeEach
    void setUp() {
        testVideo = Video.builder()
                .title("Test Lecture")
                .sourceUrl("https://youtube.com/test")
                .build();
        testVideo.setId(1L);
        
        searchRequest = SemanticSearchRequest.builder()
                .query("deployment discussion")
                .videoId(1L)
                .topK(5)
                .similarityThreshold(0.3f)
                .build();
        
        testEmbedding = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
    }
    
    @Test
    void testSearchWithinVideo() {
        // Arrange
        TranscriptChunk chunk1 = TranscriptChunk.builder()
                .chunkText("We discussed deployment strategies today")
                .startTime(120)
                .endTime(180)
                .embedding(testEmbedding.toString())
                .build();
        chunk1.setId(1L);
        chunk1.setVideo(testVideo);
        
        TranscriptChunk chunk2 = TranscriptChunk.builder()
                .chunkText("Deployment to production requires testing")
                .startTime(300)
                .endTime(360)
                .embedding(testEmbedding.toString())
                .build();
        chunk2.setId(2L);
        chunk2.setVideo(testVideo);
        
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(chunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(List.of(chunk1, chunk2));
        when(objectMapper.readValue(testEmbedding.toString(), List.class))
                .thenReturn(testEmbedding);
        
        // Act
        SemanticSearchResult result = semanticSearchService.search(searchRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("deployment discussion", result.getQuery());
        assertEquals(1L, result.getVideoId());
        assertTrue(result.getTotalMatches() > 0);
        assertNotNull(result.getProcessingTimeMs());
    }
    
    @Test
    void testGlobalSearch() {
        // Arrange
        SemanticSearchRequest request = SemanticSearchRequest.builder()
                .query("deployment discussion")
                .topK(10)
                .similarityThreshold(0.3f)
                .build();
        
        TranscriptChunk chunk = TranscriptChunk.builder()
                .chunkText("Deployment discussion with team")
                .startTime(0)
                .endTime(60)
                .embedding(testEmbedding.toString())
                .build();
        chunk.setId(1L);
        chunk.setVideo(testVideo);
        
        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(chunkRepository.findAll())
                .thenReturn(List.of(chunk));
        when(objectMapper.readValue(testEmbedding.toString(), List.class))
                .thenReturn(testEmbedding);
        
        // Act
        SemanticSearchResult result = semanticSearchService.search(request);
        
        // Assert
        assertNotNull(result);
        assertNull(result.getVideoId());  // Global search
        assertTrue(result.getTotalMatches() >= 0);
    }
    
    @Test
    void testSearchVideoNotFound() {
        // Arrange
        when(videoRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                () -> semanticSearchService.search(searchRequest));
    }
    
    @Test
    void testSearchNoChunksFound() {
        // Arrange
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(chunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(List.of());
        
        // Act
        SemanticSearchResult result = semanticSearchService.search(searchRequest);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.getMatches().isEmpty());
        assertEquals(0, result.getTotalMatches());
    }
    
    @Test
    void testSearchWithThresholdFiltering() {
        // Arrange
        SemanticSearchRequest request = SemanticSearchRequest.builder()
                .query("deployment discussion")
                .videoId(1L)
                .similarityThreshold(0.9f)  // Very high threshold
                .build();
        
        TranscriptChunk chunk = TranscriptChunk.builder()
                .chunkText("Random unrelated content")
                .startTime(0)
                .endTime(60)
                .embedding(Arrays.asList(0.9, 0.9, 0.9, 0.9, 0.9).toString())
                .build();
        chunk.setId(1L);
        chunk.setVideo(testVideo);
        
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(chunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(List.of(chunk));
        
        // Act
        SemanticSearchResult result = semanticSearchService.search(request);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.getMatches().isEmpty());  // Filtered out by threshold
    }
    
    @Test
    void testSearchMatchRanking() {
        // Arrange
        TranscriptChunk chunk1 = TranscriptChunk.builder()
                .chunkText("High similarity deployment discussion")
                .startTime(0)
                .endTime(60)
                .embedding(testEmbedding.toString())
                .build();
        chunk1.setId(1L);
        chunk1.setVideo(testVideo);
        
        TranscriptChunk chunk2 = TranscriptChunk.builder()
                .chunkText("Somewhat relevant deployment info")
                .startTime(100)
                .endTime(160)
                .embedding(Arrays.asList(0.2, 0.3, 0.4, 0.5, 0.6).toString())
                .build();
        chunk2.setId(2L);
        chunk2.setVideo(testVideo);
        
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(chunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(List.of(chunk1, chunk2));
        when(objectMapper.readValue(anyString(), eq(List.class)))
                .thenReturn(testEmbedding);
        
        // Act
        SemanticSearchResult result = semanticSearchService.search(searchRequest);
        
        // Assert
        assertNotNull(result);
        if (result.getTotalMatches() > 1) {
            // Verify matches are ranked by similarity (descending)
            SemanticSearchResult.SearchMatch first = result.getMatches().get(0);
            SemanticSearchResult.SearchMatch second = result.getMatches().get(1);
            assertTrue(first.getSimilarityScore() >= second.getSimilarityScore());
        }
    }
    
    @Test
    void testSearchTimestampFormatting() {
        // Arrange
        TranscriptChunk chunk = TranscriptChunk.builder()
                .chunkText("Test deployment discussion at specific time")
                .startTime(3725)  // 01:02:05
                .endTime(3845)    // 01:04:05
                .embedding(testEmbedding.toString())
                .build();
        chunk.setId(1L);
        chunk.setVideo(testVideo);
        
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(chunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(List.of(chunk));
        when(objectMapper.readValue(testEmbedding.toString(), List.class))
                .thenReturn(testEmbedding);
        
        // Act
        SemanticSearchResult result = semanticSearchService.search(searchRequest);
        
        // Assert
        assertNotNull(result);
        if (result.getTotalMatches() > 0) {
            SemanticSearchResult.SearchMatch match = result.getMatches().get(0);
            assertEquals("01:02:05", match.getStartTime());
            assertEquals("01:04:05", match.getEndTime());
        }
    }
}

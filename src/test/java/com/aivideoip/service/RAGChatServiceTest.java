package com.aivideoip.service;

import com.aivideoip.dto.ChatRequest;
import com.aivideoip.dto.ChatResponse;
import com.aivideoip.entity.TranscriptChunk;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.TranscriptChunkRepository;
import com.aivideoip.repository.VideoRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RAGChatServiceTest {
    
    @Mock
    private OllamaEmbeddingClient embeddingClient;
    
    @Mock
    private OllamaClient ollamaClient;
    
    @Mock
    private TranscriptChunkRepository chunkRepository;
    
    @Mock
    private VideoRepository videoRepository;
    
    @InjectMocks
    private RAGChatService ragChatService;
    
    private Video testVideo;
    private ChatRequest chatRequest;
    private List<Double> testEmbedding;
    
    @BeforeEach
    void setUp() {
        testVideo = Video.builder()
                .title("Test Video")
                .sourceUrl("https://youtube.com/test")
                .build();
        testVideo.setId(1L);
        
        chatRequest = ChatRequest.builder()
                .videoId(1L)
                .question("What is the main topic?")
                .topK(5)
                .similarityThreshold(0.5f)
                .build();
        
        testEmbedding = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
    }
    
    @Test
    void testProcessQuerySuccess() {
        // Arrange
        TranscriptChunk chunk = TranscriptChunk.builder()
                .chunkText("This is a test chunk")
                .startTime(0)
                .endTime(10)
                .embedding(testEmbedding.toString())
                .build();
        chunk.setId(1L);
        
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(chunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(List.of(chunk));
        when(ollamaClient.generateText(anyString(), anyString()))
                .thenReturn(Mono.just("Answer: The main topic is about testing."));
        
        // Act
        ChatResponse response = ragChatService.processQuery(chatRequest);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertTrue(response.getSources().size() > 0);
        assertNotNull(response.getConfidence());
        assertTrue(response.getConfidence() > 0);
    }
    
    @Test
    void testProcessQueryVideoNotFound() {
        // Arrange
        when(videoRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> ragChatService.processQuery(chatRequest));
    }
    
    @Test
    void testProcessQueryNoChunks() {
        // Arrange
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(chunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(List.of());
        
        // Act
        ChatResponse response = ragChatService.processQuery(chatRequest);
        
        // Assert
        assertNotNull(response);
        assertTrue(response.getSources().isEmpty());
        assertEquals(0f, response.getConfidence());
    }
    
    @Test
    void testProcessQueryWithDefaultTopK() {
        // Arrange
        ChatRequest request = ChatRequest.builder()
                .videoId(1L)
                .question("What is the main topic?")
                .build();
        
        TranscriptChunk chunk = TranscriptChunk.builder()
                .chunkText("Test chunk")
                .startTime(0)
                .endTime(10)
                .embedding(testEmbedding.toString())
                .build();
        chunk.setId(1L);
        
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(chunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(List.of(chunk));
        when(ollamaClient.generateText(anyString(), anyString()))
                .thenReturn(Mono.just("Answer"));
        
        // Act
        ChatResponse response = ragChatService.processQuery(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("Answer", response.getAnswer());
    }
    
    @Test
    void testProcessQueryWithThresholdFiltering() {
        // Arrange
        ChatRequest request = ChatRequest.builder()
                .videoId(1L)
                .question("What is the main topic?")
                .similarityThreshold(0.9f)
                .build();
        
        TranscriptChunk chunk = TranscriptChunk.builder()
                .chunkText("Low similarity chunk")
                .startTime(0)
                .endTime(10)
                .embedding(Arrays.asList(0.9, 0.9, 0.9, 0.9, 0.9).toString())
                .build();
        chunk.setId(1L);
        
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(chunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(List.of(chunk));
        
        // Act
        ChatResponse response = ragChatService.processQuery(request);
        
        // Assert
        assertNotNull(response);
        // Should return empty response due to high threshold filtering
        assertTrue(response.getSources().isEmpty());
    }
    
    @Test
    void testProcessQueryEmbeddingGenerationFailure() {
        // Arrange
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(Mono.just(List.of()));
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> ragChatService.processQuery(chatRequest));
    }
}

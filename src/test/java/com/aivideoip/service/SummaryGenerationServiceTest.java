package com.aivideoip.service;

import com.aivideoip.dto.SummaryDTO;
import com.aivideoip.entity.Summary;
import com.aivideoip.entity.TranscriptChunk;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.SummaryRepository;
import com.aivideoip.repository.TranscriptChunkRepository;
import com.aivideoip.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SummaryGenerationService
 * Tests comprehensive summary generation with LLM integration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryGenerationService Tests")
class SummaryGenerationServiceTest {

    @Mock
    private TranscriptChunkRepository transcriptChunkRepository;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private SummaryRepository summaryRepository;

    @Mock
    private OllamaClient ollamaClient;

    @InjectMocks
    private SummaryGenerationService summaryGenerationService;

    private Video testVideo;
    private List<TranscriptChunk> testChunks;
    private Summary testSummary;

    @BeforeEach
    void setUp() {
        // Create test video
        testVideo = Video.builder()
                .title("Test Lecture")
                .description("AI and Machine Learning Basics")
                .sourceUrl("https://youtube.com/watch?v=test")
                .durationSeconds(3600L)
                .source(Video.VideoSource.YOUTUBE)
                .build();
        testVideo.setId(1L);

        // Create test transcript chunks
        testChunks = new ArrayList<>();
        TranscriptChunk chunk1 = TranscriptChunk.builder()
                .video(testVideo)
                .content("Introduction to machine learning and AI concepts")
                .startTime(0)
                .endTime(300)
                .build();
        chunk1.setId(1L);

        TranscriptChunk chunk2 = TranscriptChunk.builder()
                .video(testVideo)
                .content("Deep learning neural networks and transformers")
                .startTime(300)
                .endTime(600)
                .build();
        chunk2.setId(2L);

        testChunks.add(chunk1);
        testChunks.add(chunk2);

        // Create test summary
        testSummary = Summary.builder()
                .video(testVideo)
                .content("Comprehensive AI and ML overview")
                .build();
        testSummary.setId(1L);
    }

    @Test
    @DisplayName("Should generate comprehensive summary successfully")
    void testGenerateComprehensiveSummary_Success() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(testChunks);

        String mockLLMResponse = """
                {
                    "notes": ["Point 1", "Point 2", "Point 3"],
                    "concepts": ["Concept 1", "Concept 2"],
                    "actionItems": ["Action 1", "Action 2"],
                    "timestamps": ["00:01:30", "00:05:45"]
                }
                """;

        when(ollamaClient.generateTextSync(anyString())).thenReturn(mockLLMResponse);
        when(summaryRepository.save(any(Summary.class))).thenReturn(testSummary);

        // When
        SummaryDTO result = summaryGenerationService.generateComprehensiveSummary(1L);

        // Then
        assertNotNull(result);
        assertEquals(testVideo.getId(), result.getVideoId());
        verify(videoRepository, times(1)).findById(1L);
        verify(transcriptChunkRepository, times(1)).findByVideoIdOrderByStartTime(1L);
        verify(ollamaClient, times(1)).generateTextSync(anyString());
        verify(summaryRepository, times(1)).save(any(Summary.class));
    }

    @Test
    @DisplayName("Should throw exception when video not found during summary generation")
    void testGenerateComprehensiveSummary_VideoNotFound() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                summaryGenerationService.generateComprehensiveSummary(1L));
        verify(transcriptChunkRepository, never()).findByVideoIdOrderByStartTime(anyLong());
    }

    @Test
    @DisplayName("Should handle empty transcript chunks")
    void testGenerateComprehensiveSummary_EmptyTranscript() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(new ArrayList<>());

        // When & Then
        assertThrows(IllegalStateException.class, () ->
                summaryGenerationService.generateComprehensiveSummary(1L));
    }

    @Test
    @DisplayName("Should generate summary by type BRIEF")
    void testGenerateSummaryByType_Brief() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(testChunks);

        String mockResponse = "Brief summary of the content";
        when(ollamaClient.generateTextSync(contains("Brief"))).thenReturn(mockResponse);
        when(summaryRepository.save(any(Summary.class))).thenReturn(testSummary);

        // When
        SummaryDTO result = summaryGenerationService.generateSummaryByType(1L, "BRIEF");

        // Then
        assertNotNull(result);
        verify(ollamaClient, times(1)).generateTextSync(anyString());
        verify(summaryRepository, times(1)).save(any(Summary.class));
    }

    @Test
    @DisplayName("Should generate summary by type DETAILED")
    void testGenerateSummaryByType_Detailed() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(testChunks);

        String mockResponse = "Detailed comprehensive summary with all details";
        when(ollamaClient.generateTextSync(contains("Detailed"))).thenReturn(mockResponse);
        when(summaryRepository.save(any(Summary.class))).thenReturn(testSummary);

        // When
        SummaryDTO result = summaryGenerationService.generateSummaryByType(1L, "DETAILED");

        // Then
        assertNotNull(result);
        verify(ollamaClient, times(1)).generateTextSync(anyString());
        verify(summaryRepository, times(1)).save(any(Summary.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid summary type")
    void testGenerateSummaryByType_InvalidType() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(testChunks);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                summaryGenerationService.generateSummaryByType(1L, "INVALID_TYPE"));
    }

    @Test
    @DisplayName("Should extract action items with timestamps")
    void testExtractActionItemsWithTimestamps() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(testChunks);

        String mockResponse = """
                [
                    {
                        "action": "Review chapter 2",
                        "timestamp": "00:05:30",
                        "owner": "Team",
                        "priority": "High"
                    },
                    {
                        "action": "Complete assignment",
                        "timestamp": "00:15:45",
                        "owner": "Student",
                        "priority": "Medium"
                    }
                ]
                """;

        when(ollamaClient.generateTextSync(anyString())).thenReturn(mockResponse);

        // When
        List<Map<String, Object>> result = summaryGenerationService.extractActionItemsWithTimestamps(1L);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(videoRepository, times(1)).findById(1L);
        verify(transcriptChunkRepository, times(1)).findByVideoIdOrderByStartTime(1L);
        verify(ollamaClient, times(1)).generateTextSync(anyString());
    }

    @Test
    @DisplayName("Should extract key concepts with importance scores")
    void testExtractKeyConcepts() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(testChunks);

        String mockResponse = """
                [
                    {
                        "concept": "Machine Learning",
                        "explanation": "The field of AI focused on algorithms",
                        "importance": 9.5
                    },
                    {
                        "concept": "Neural Networks",
                        "explanation": "Computational models inspired by brain",
                        "importance": 8.7
                    }
                ]
                """;

        when(ollamaClient.generateTextSync(anyString())).thenReturn(mockResponse);

        // When
        List<Map<String, Object>> result = summaryGenerationService.extractKeyConcepts(1L);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(videoRepository, times(1)).findById(1L);
        verify(transcriptChunkRepository, times(1)).findByVideoIdOrderByStartTime(1L);
        verify(ollamaClient, times(1)).generateTextSync(anyString());
    }

    @Test
    @DisplayName("Should generate comprehensive summary asynchronously")
    void testGenerateComprehensiveSummaryAsync() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(testChunks);

        String mockLLMResponse = """
                {
                    "notes": ["Async Point 1", "Async Point 2"],
                    "concepts": ["Async Concept"],
                    "actionItems": ["Async Action"],
                    "timestamps": ["00:02:00"]
                }
                """;

        when(ollamaClient.generateText(anyString())).thenReturn(Mono.just(mockLLMResponse));
        when(summaryRepository.save(any(Summary.class))).thenReturn(testSummary);

        // When
        Mono<SummaryDTO> result = summaryGenerationService.generateComprehensiveSummaryAsync(1L);

        // Then
        assertNotNull(result);
        SummaryDTO dto = result.block();
        assertNotNull(dto);
        assertEquals(testVideo.getId(), dto.getVideoId());
        verify(videoRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should handle LLM service failure gracefully")
    void testGenerateComprehensiveSummary_LLMFailure() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(testChunks);
        when(ollamaClient.generateTextSync(anyString()))
                .thenThrow(new RuntimeException("LLM service unavailable"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                summaryGenerationService.generateComprehensiveSummary(1L));
    }

    @Test
    @DisplayName("Should build valid prompt from transcript chunks")
    void testPromptConstruction() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(testChunks);

        String mockResponse = "Summary";
        when(ollamaClient.generateTextSync(anyString())).thenReturn(mockResponse);
        when(summaryRepository.save(any(Summary.class))).thenReturn(testSummary);

        // When
        summaryGenerationService.generateComprehensiveSummary(1L);

        // Then - Verify that generateTextSync was called with a non-empty prompt
        verify(ollamaClient, times(1)).generateTextSync(argThat(prompt ->
                prompt != null &&
                !prompt.isEmpty() &&
                prompt.contains("Introduction to machine learning") // content from first chunk
        ));
    }

    @Test
    @DisplayName("Should persist summary to database")
    void testSummaryPersistence() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.findByVideoIdOrderByStartTime(1L))
                .thenReturn(testChunks);
        when(ollamaClient.generateTextSync(anyString())).thenReturn("Summary");
        when(summaryRepository.save(any(Summary.class))).thenReturn(testSummary);

        // When
        SummaryDTO result = summaryGenerationService.generateComprehensiveSummary(1L);

        // Then
        assertNotNull(result);
        verify(summaryRepository, times(1)).save(argThat(summary ->
                summary.getVideo().getId().equals(1L) &&
                summary.getSummaryType() == Summary.SummaryType.COMPREHENSIVE
        ));
    }
}

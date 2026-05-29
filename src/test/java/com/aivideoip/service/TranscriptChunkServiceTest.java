package com.aivideoip.service;

import com.aivideoip.dto.TranscriptChunkDTO;
import com.aivideoip.entity.TranscriptChunk;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.TranscriptChunkRepository;
import com.aivideoip.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TranscriptChunkService
 * Tests transcript chunking and retrieval operations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TranscriptChunkService Tests")
class TranscriptChunkServiceTest {

    @Mock
    private TranscriptChunkRepository transcriptChunkRepository;

    @Mock
    private VideoRepository videoRepository;

    @InjectMocks
    private TranscriptChunkService transcriptChunkService;

    private Video testVideo;
    private List<TranscriptChunk> testChunks;
    private TranscriptChunkDTO testChunkDTO;

    @BeforeEach
    void setUp() {
        // Create test video
        testVideo = Video.builder()
                .id(1L)
                .title("Test Lecture")
                .youtubeUrl("https://youtube.com/watch?v=test")
                .duration(3600)
                .build();

        // Create test transcript chunks
        testChunks = new ArrayList<>();
        
        TranscriptChunk chunk1 = TranscriptChunk.builder()
                .id(1L)
                .video(testVideo)
                .content("This is the first part of the transcript discussing fundamentals")
                .startTime(0)
                .endTime(300)
                .chunkIndex(0)
                .tokenCount(50)
                .build();

        TranscriptChunk chunk2 = TranscriptChunk.builder()
                .id(2L)
                .video(testVideo)
                .content("This is the second part discussing advanced concepts")
                .startTime(300)
                .endTime(600)
                .chunkIndex(1)
                .tokenCount(55)
                .build();

        testChunks.add(chunk1);
        testChunks.add(chunk2);

        // Create test DTO
        testChunkDTO = TranscriptChunkDTO.builder()
                .videoId(1L)
                .content("This is a test chunk")
                .startTime(0)
                .endTime(300)
                .chunkIndex(0)
                .tokenCount(50)
                .build();
    }

    @Test
    @DisplayName("Should save transcript chunk successfully")
    void testSaveTranscriptChunk_Success() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        TranscriptChunk chunk = testChunks.get(0);
        when(transcriptChunkRepository.save(any(TranscriptChunk.class))).thenReturn(chunk);

        // When
        TranscriptChunkDTO result = transcriptChunkService.saveTranscriptChunk(testChunkDTO);

        // Then
        assertNotNull(result);
        assertEquals(testChunkDTO.getContent(), result.getContent());
        assertEquals(testChunkDTO.getStartTime(), result.getStartTime());
        assertEquals(testChunkDTO.getEndTime(), result.getEndTime());
        verify(transcriptChunkRepository, times(1)).save(any(TranscriptChunk.class));
    }

    @Test
    @DisplayName("Should throw exception when video not found during save")
    void testSaveTranscriptChunk_VideoNotFound() {
        // Given
        testChunkDTO.setVideoId(999L);
        when(videoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                transcriptChunkService.saveTranscriptChunk(testChunkDTO));
        verify(transcriptChunkRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve transcript chunks by video ID")
    void testGetChunksByVideoId_Success() {
        // Given
        when(transcriptChunkRepository.findByVideoIdOrderByChunkIndex(1L))
                .thenReturn(testChunks);

        // When
        List<TranscriptChunkDTO> result = transcriptChunkService.getChunksByVideoId(1L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("This is the first part of the transcript discussing fundamentals", result.get(0).getContent());
        assertEquals("This is the second part discussing advanced concepts", result.get(1).getContent());
        verify(transcriptChunkRepository, times(1)).findByVideoIdOrderByChunkIndex(1L);
    }

    @Test
    @DisplayName("Should return empty list when no chunks exist for video")
    void testGetChunksByVideoId_Empty() {
        // Given
        when(transcriptChunkRepository.findByVideoIdOrderByChunkIndex(1L))
                .thenReturn(new ArrayList<>());

        // When
        List<TranscriptChunkDTO> result = transcriptChunkService.getChunksByVideoId(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should retrieve chunk by ID successfully")
    void testGetChunkById_Success() {
        // Given
        TranscriptChunk chunk = testChunks.get(0);
        when(transcriptChunkRepository.findById(1L)).thenReturn(Optional.of(chunk));

        // When
        TranscriptChunkDTO result = transcriptChunkService.getChunkById(1L);

        // Then
        assertNotNull(result);
        assertEquals(chunk.getContent(), result.getContent());
        assertEquals(chunk.getStartTime(), result.getStartTime());
        verify(transcriptChunkRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when chunk not found by ID")
    void testGetChunkById_NotFound() {
        // Given
        when(transcriptChunkRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                transcriptChunkService.getChunkById(1L));
    }

    @Test
    @DisplayName("Should update transcript chunk successfully")
    void testUpdateChunk_Success() {
        // Given
        TranscriptChunk existingChunk = testChunks.get(0);
        TranscriptChunkDTO updatedDTO = TranscriptChunkDTO.builder()
                .videoId(1L)
                .content("Updated content with new information")
                .startTime(0)
                .endTime(350)
                .chunkIndex(0)
                .tokenCount(60)
                .build();

        TranscriptChunk updatedChunk = TranscriptChunk.builder()
                .id(1L)
                .video(testVideo)
                .content("Updated content with new information")
                .startTime(0)
                .endTime(350)
                .chunkIndex(0)
                .tokenCount(60)
                .build();

        when(transcriptChunkRepository.findById(1L)).thenReturn(Optional.of(existingChunk));
        when(transcriptChunkRepository.save(any(TranscriptChunk.class))).thenReturn(updatedChunk);

        // When
        TranscriptChunkDTO result = transcriptChunkService.updateChunk(1L, updatedDTO);

        // Then
        assertNotNull(result);
        assertEquals("Updated content with new information", result.getContent());
        assertEquals(350, result.getEndTime());
        verify(transcriptChunkRepository, times(1)).save(any(TranscriptChunk.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent chunk")
    void testUpdateChunk_NotFound() {
        // Given
        when(transcriptChunkRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                transcriptChunkService.updateChunk(1L, testChunkDTO));
        verify(transcriptChunkRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete chunk successfully")
    void testDeleteChunk_Success() {
        // Given
        when(transcriptChunkRepository.existsById(1L)).thenReturn(true);

        // When
        transcriptChunkService.deleteChunk(1L);

        // Then
        verify(transcriptChunkRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent chunk")
    void testDeleteChunk_NotFound() {
        // Given
        when(transcriptChunkRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                transcriptChunkService.deleteChunk(1L));
        verify(transcriptChunkRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should delete all chunks for a video")
    void testDeleteChunksByVideoId() {
        // Given
        when(transcriptChunkRepository.findByVideoIdOrderByChunkIndex(1L))
                .thenReturn(testChunks);

        // When
        transcriptChunkService.deleteChunksByVideoId(1L);

        // Then
        verify(transcriptChunkRepository, times(2)).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should validate chunk timing")
    void testValidateChunkTiming() {
        // Given - startTime should be less than endTime
        TranscriptChunkDTO invalidChunk = TranscriptChunkDTO.builder()
                .videoId(1L)
                .content("Test content")
                .startTime(600)
                .endTime(300)  // Invalid: end before start
                .chunkIndex(0)
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                transcriptChunkService.saveTranscriptChunk(invalidChunk));
    }

    @Test
    @DisplayName("Should handle chunk with zero duration")
    void testChunkZeroDuration() {
        // Given
        TranscriptChunkDTO zeroDurationChunk = TranscriptChunkDTO.builder()
                .videoId(1L)
                .content("Test content")
                .startTime(300)
                .endTime(300)  // Zero duration
                .chunkIndex(0)
                .build();

        // When & Then - Should allow zero duration chunks (single moment)
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.save(any(TranscriptChunk.class)))
                .thenReturn(TranscriptChunk.builder()
                        .id(1L)
                        .video(testVideo)
                        .content("Test content")
                        .startTime(300)
                        .endTime(300)
                        .chunkIndex(0)
                        .build());

        TranscriptChunkDTO result = transcriptChunkService.saveTranscriptChunk(zeroDurationChunk);
        assertNotNull(result);
        assertEquals(0, result.getEndTime() - result.getStartTime());
    }

    @Test
    @DisplayName("Should preserve chunk order by index")
    void testChunkOrderPreservation() {
        // Given
        when(transcriptChunkRepository.findByVideoIdOrderByChunkIndex(1L))
                .thenReturn(testChunks);

        // When
        List<TranscriptChunkDTO> result = transcriptChunkService.getChunksByVideoId(1L);

        // Then
        assertEquals(0, result.get(0).getChunkIndex());
        assertEquals(1, result.get(1).getChunkIndex());
        assertTrue(result.get(0).getStartTime() <= result.get(1).getStartTime());
    }

    @Test
    @DisplayName("Should map TranscriptChunk entity to DTO correctly")
    void testMapToDTO() {
        // Given
        TranscriptChunk chunk = testChunks.get(0);
        when(transcriptChunkRepository.findById(1L)).thenReturn(Optional.of(chunk));

        // When
        TranscriptChunkDTO result = transcriptChunkService.getChunkById(1L);

        // Then
        assertNotNull(result);
        assertEquals(chunk.getContent(), result.getContent());
        assertEquals(chunk.getStartTime(), result.getStartTime());
        assertEquals(chunk.getEndTime(), result.getEndTime());
        assertEquals(chunk.getChunkIndex(), result.getChunkIndex());
        assertEquals(chunk.getTokenCount(), result.getTokenCount());
    }

    @Test
    @DisplayName("Should handle large content chunks")
    void testLargeContentChunk() {
        // Given
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("This is a long piece of text. ");
        }

        TranscriptChunkDTO largeChunkDTO = TranscriptChunkDTO.builder()
                .videoId(1L)
                .content(largeContent.toString())
                .startTime(0)
                .endTime(3600)
                .chunkIndex(0)
                .tokenCount(5000)
                .build();

        TranscriptChunk largeChunk = TranscriptChunk.builder()
                .id(1L)
                .video(testVideo)
                .content(largeContent.toString())
                .startTime(0)
                .endTime(3600)
                .chunkIndex(0)
                .tokenCount(5000)
                .build();

        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.save(any(TranscriptChunk.class))).thenReturn(largeChunk);

        // When
        TranscriptChunkDTO result = transcriptChunkService.saveTranscriptChunk(largeChunkDTO);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().length() > 10000);
    }

    @Test
    @DisplayName("Should retrieve chunks within time range")
    void testGetChunksInTimeRange() {
        // Given
        when(transcriptChunkRepository.findByVideoIdOrderByChunkIndex(1L))
                .thenReturn(testChunks);

        // When - Filter chunks between 200 and 400 seconds
        List<TranscriptChunkDTO> allChunks = transcriptChunkService.getChunksByVideoId(1L);
        List<TranscriptChunkDTO> filtered = allChunks.stream()
                .filter(chunk -> chunk.getStartTime() >= 200 && chunk.getEndTime() <= 400)
                .toList();

        // Then
        assertEquals(1, filtered.size());
        assertEquals(300, filtered.get(0).getStartTime());
    }

    @Test
    @DisplayName("Should save multiple chunks in batch")
    void testSaveMultipleChunks() {
        // Given
        List<TranscriptChunkDTO> chunkDTOs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            chunkDTOs.add(TranscriptChunkDTO.builder()
                    .videoId(1L)
                    .content("Chunk " + i)
                    .startTime(i * 300)
                    .endTime((i + 1) * 300)
                    .chunkIndex(i)
                    .build());
        }

        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(transcriptChunkRepository.save(any(TranscriptChunk.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        for (TranscriptChunkDTO chunkDTO : chunkDTOs) {
            transcriptChunkService.saveTranscriptChunk(chunkDTO);
        }

        // Then
        verify(transcriptChunkRepository, times(5)).save(any(TranscriptChunk.class));
    }
}

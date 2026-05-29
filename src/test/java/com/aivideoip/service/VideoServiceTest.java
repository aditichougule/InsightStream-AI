package com.aivideoip.service;

import com.aivideoip.dto.VideoDTO;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
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
 * Unit tests for VideoService
 * Tests CRUD operations for videos and video-related operations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VideoService Tests")
class VideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @InjectMocks
    private VideoService videoService;

    private Video testVideo;
    private VideoDTO testVideoDTO;

    @BeforeEach
    void setUp() {
        // Create test video
        testVideo = Video.builder()
                .id(1L)
                .title("AI and Machine Learning Fundamentals")
                .description("Complete guide to AI and ML")
                .youtubeUrl("https://youtube.com/watch?v=test123")
                .duration(3600)
                .thumbnail("https://example.com/thumb.jpg")
                .build();

        // Create test DTO
        testVideoDTO = VideoDTO.builder()
                .id(1L)
                .title("AI and Machine Learning Fundamentals")
                .description("Complete guide to AI and ML")
                .youtubeUrl("https://youtube.com/watch?v=test123")
                .duration(3600)
                .thumbnail("https://example.com/thumb.jpg")
                .build();
    }

    @Test
    @DisplayName("Should create video successfully")
    void testCreateVideo_Success() {
        // Given
        when(videoRepository.save(any(Video.class))).thenReturn(testVideo);

        // When
        VideoDTO result = videoService.createVideo(testVideoDTO);

        // Then
        assertNotNull(result);
        assertEquals(testVideoDTO.getTitle(), result.getTitle());
        assertEquals(testVideoDTO.getDescription(), result.getDescription());
        assertEquals(testVideoDTO.getYoutubeUrl(), result.getYoutubeUrl());
        assertEquals(testVideoDTO.getDuration(), result.getDuration());
        verify(videoRepository, times(1)).save(any(Video.class));
    }

    @Test
    @DisplayName("Should retrieve video by ID successfully")
    void testGetVideoById_Success() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));

        // When
        VideoDTO result = videoService.getVideoById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testVideoDTO.getId(), result.getId());
        assertEquals(testVideoDTO.getTitle(), result.getTitle());
        verify(videoRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when video not found by ID")
    void testGetVideoById_NotFound() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                videoService.getVideoById(1L));
    }

    @Test
    @DisplayName("Should retrieve all videos successfully")
    void testGetAllVideos_Success() {
        // Given
        List<Video> videos = new ArrayList<>();
        videos.add(testVideo);

        Video video2 = Video.builder()
                .id(2L)
                .title("Deep Learning Advanced")
                .description("Advanced DL concepts")
                .youtubeUrl("https://youtube.com/watch?v=test456")
                .duration(5400)
                .build();
        videos.add(video2);

        when(videoRepository.findAll()).thenReturn(videos);

        // When
        List<VideoDTO> result = videoService.getAllVideos();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("AI and Machine Learning Fundamentals", result.get(0).getTitle());
        assertEquals("Deep Learning Advanced", result.get(1).getTitle());
        verify(videoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no videos exist")
    void testGetAllVideos_Empty() {
        // Given
        when(videoRepository.findAll()).thenReturn(new ArrayList<>());

        // When
        List<VideoDTO> result = videoService.getAllVideos();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(videoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should update video successfully")
    void testUpdateVideo_Success() {
        // Given
        VideoDTO updatedDTO = VideoDTO.builder()
                .id(1L)
                .title("Updated Title")
                .description("Updated description")
                .youtubeUrl("https://youtube.com/watch?v=updated")
                .duration(4200)
                .build();

        Video updatedVideo = Video.builder()
                .id(1L)
                .title("Updated Title")
                .description("Updated description")
                .youtubeUrl("https://youtube.com/watch?v=updated")
                .duration(4200)
                .build();

        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(videoRepository.save(any(Video.class))).thenReturn(updatedVideo);

        // When
        VideoDTO result = videoService.updateVideo(1L, updatedDTO);

        // Then
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated description", result.getDescription());
        verify(videoRepository, times(1)).save(any(Video.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent video")
    void testUpdateVideo_NotFound() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                videoService.updateVideo(1L, testVideoDTO));
        verify(videoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete video successfully")
    void testDeleteVideo_Success() {
        // Given
        when(videoRepository.existsById(1L)).thenReturn(true);

        // When
        videoService.deleteVideo(1L);

        // Then
        verify(videoRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent video")
    void testDeleteVideo_NotFound() {
        // Given
        when(videoRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                videoService.deleteVideo(1L));
        verify(videoRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should validate YouTube URL format")
    void testCreateVideo_InvalidYoutubeUrl() {
        // Given
        VideoDTO invalidDTO = VideoDTO.builder()
                .title("Test Video")
                .description("Test")
                .youtubeUrl("not-a-valid-url")
                .duration(3600)
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                videoService.createVideo(invalidDTO));
    }

    @Test
    @DisplayName("Should validate duration is positive")
    void testCreateVideo_InvalidDuration() {
        // Given
        VideoDTO invalidDTO = VideoDTO.builder()
                .title("Test Video")
                .description("Test")
                .youtubeUrl("https://youtube.com/watch?v=test")
                .duration(-100)
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                videoService.createVideo(invalidDTO));
    }

    @Test
    @DisplayName("Should map Video entity to DTO correctly")
    void testMapToDTO() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));

        // When
        VideoDTO result = videoService.getVideoById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testVideo.getId(), result.getId());
        assertEquals(testVideo.getTitle(), result.getTitle());
        assertEquals(testVideo.getDescription(), result.getDescription());
        assertEquals(testVideo.getYoutubeUrl(), result.getYoutubeUrl());
        assertEquals(testVideo.getDuration(), result.getDuration());
        assertEquals(testVideo.getThumbnail(), result.getThumbnail());
    }

    @Test
    @DisplayName("Should handle partial video update")
    void testUpdateVideo_PartialUpdate() {
        // Given
        VideoDTO partialDTO = VideoDTO.builder()
                .title("New Title")
                .build();

        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(videoRepository.save(any(Video.class))).thenReturn(testVideo);

        // When
        VideoDTO result = videoService.updateVideo(1L, partialDTO);

        // Then
        assertNotNull(result);
        verify(videoRepository, times(1)).save(any(Video.class));
    }

    @Test
    @DisplayName("Should handle video with long title")
    void testCreateVideo_LongTitle() {
        // Given
        String longTitle = "A".repeat(500);
        VideoDTO dtoWithLongTitle = VideoDTO.builder()
                .title(longTitle)
                .description("Test")
                .youtubeUrl("https://youtube.com/watch?v=test")
                .duration(3600)
                .build();

        Video videoWithLongTitle = Video.builder()
                .id(1L)
                .title(longTitle)
                .description("Test")
                .youtubeUrl("https://youtube.com/watch?v=test")
                .duration(3600)
                .build();

        when(videoRepository.save(any(Video.class))).thenReturn(videoWithLongTitle);

        // When
        VideoDTO result = videoService.createVideo(dtoWithLongTitle);

        // Then
        assertNotNull(result);
        assertEquals(longTitle, result.getTitle());
    }

    @Test
    @DisplayName("Should retrieve video by YouTube URL")
    void testFindByYoutubeUrl() {
        // Given
        String youtubeUrl = "https://youtube.com/watch?v=test123";
        when(videoRepository.findByYoutubeUrl(youtubeUrl)).thenReturn(Optional.of(testVideo));

        // When
        Optional<Video> result = videoRepository.findByYoutubeUrl(youtubeUrl);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testVideo.getTitle(), result.get().getTitle());
    }

    @Test
    @DisplayName("Should handle video duration conversion correctly")
    void testVideoDurationHandling() {
        // Given - Test various duration values
        Video shortVideo = Video.builder()
                .id(1L)
                .title("Short Video")
                .duration(60)  // 1 minute
                .youtubeUrl("https://youtube.com/watch?v=short")
                .build();

        Video longVideo = Video.builder()
                .id(2L)
                .title("Long Video")
                .duration(86400)  // 24 hours
                .youtubeUrl("https://youtube.com/watch?v=long")
                .build();

        when(videoRepository.findById(1L)).thenReturn(Optional.of(shortVideo));
        when(videoRepository.findById(2L)).thenReturn(Optional.of(longVideo));

        // When
        VideoDTO shortResult = videoService.getVideoById(1L);
        VideoDTO longResult = videoService.getVideoById(2L);

        // Then
        assertEquals(60, shortResult.getDuration());
        assertEquals(86400, longResult.getDuration());
    }

    @Test
    @DisplayName("Should preserve video metadata during operations")
    void testPreserveVideoMetadata() {
        // Given
        String thumbnail = "https://example.com/thumb.jpg";
        testVideo.setThumbnail(thumbnail);

        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));

        // When
        VideoDTO result = videoService.getVideoById(1L);

        // Then
        assertNotNull(result.getThumbnail());
        assertEquals(thumbnail, result.getThumbnail());
    }
}

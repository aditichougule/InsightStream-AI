package com.aivideoip.service;

import com.aivideoip.dto.CreateVideoRequest;
import com.aivideoip.dto.VideoDTO;
import com.aivideoip.entity.Video;
import com.aivideoip.entity.User;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for video operations
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserService userService;

    public VideoDTO createVideo(CreateVideoRequest request, Long userId) {
        log.info("Creating video for user: {}", userId);

        User owner = userService.getUserById(userId);

        Video video = new Video();
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setSourceUrl(request.getSourceUrl());
        video.setSource(Video.VideoSource.valueOf(request.getSource().toUpperCase()));
        video.setThumbnailUrl(request.getThumbnailUrl());
        video.setDurationSeconds(request.getDurationSeconds());
        video.setOwner(owner);
        video.setProcessingStatus(Video.ProcessingStatus.PENDING);

        Video savedVideo = videoRepository.save(video);
        log.info("Video created with ID: {}", savedVideo.getId());

        return convertToDTO(savedVideo);
    }

    public VideoDTO getVideoById(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));
        return convertToDTO(video);
    }

    public Page<VideoDTO> getUserVideos(Long userId, Pageable pageable) {
        User owner = userService.getUserById(userId);
        Page<Video> videos = videoRepository.findByOwner(owner, pageable);
        return videos.map(this::convertToDTO);
    }

    public VideoDTO updateVideo(Long videoId, CreateVideoRequest request) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));

        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setThumbnailUrl(request.getThumbnailUrl());

        Video updatedVideo = videoRepository.save(video);
        log.info("Video updated: {}", videoId);

        return convertToDTO(updatedVideo);
    }

    public void deleteVideo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));

        video.setActive(false);
        videoRepository.save(video);
        log.info("Video deleted: {}", videoId);
    }

    public VideoDTO convertToDTO(Video video) {
        return VideoDTO.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .sourceUrl(video.getSourceUrl())
                .source(video.getSource().name())
                .thumbnailUrl(video.getThumbnailUrl())
                .durationSeconds(video.getDurationSeconds())
                .ownerId(video.getOwner().getId())
                .processingStatus(video.getProcessingStatus().name())
                .errorMessage(video.getErrorMessage())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .build();
    }

    public void updateProcessingStatus(Long videoId, Video.ProcessingStatus status) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));

        video.setProcessingStatus(status);
        videoRepository.save(video);
        log.info("Video processing status updated: {} -> {}", videoId, status);
    }

    public void setErrorMessage(Long videoId, String errorMessage) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));

        video.setErrorMessage(errorMessage);
        video.setProcessingStatus(Video.ProcessingStatus.FAILED);
        videoRepository.save(video);
        log.error("Video processing failed: {} - Error: {}", videoId, errorMessage);
    }
}

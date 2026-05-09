package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.CreateVideoRequest;
import com.aivideoip.dto.VideoDTO;
import com.aivideoip.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Videos", description = "Manage videos and related operations")
public class VideoController {

    private final VideoService videoService;

    @PostMapping
    @Operation(summary = "Upload and create a new video")
    public ResponseEntity<ApiResponse<VideoDTO>> createVideo(
            @Valid @RequestBody CreateVideoRequest request,
            @RequestParam Long userId) {
        log.info("New video upload from user: {}", userId);
        var video = videoService.createVideo(request, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(video, "Video created successfully"));
    }

    @GetMapping("/{videoId}")
    @Operation(summary = "Retrieve video details")
    public ResponseEntity<ApiResponse<VideoDTO>> getVideo(@PathVariable Long videoId) {
        log.debug("Fetching video: {}", videoId);
        return ResponseEntity.ok(ApiResponse.success(videoService.getVideoById(videoId)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all videos for a user with pagination")
    public ResponseEntity<ApiResponse<Page<VideoDTO>>> listUserVideos(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.debug("Listing videos for user {}, page {}", userId, page);
        var pageable = PageRequest.of(page, size);
        var videos = videoService.getUserVideos(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(videos));
    }

    @PutMapping("/{videoId}")
    @Operation(summary = "Update video metadata")
    public ResponseEntity<ApiResponse<VideoDTO>> updateVideo(
            @PathVariable Long videoId,
            @Valid @RequestBody CreateVideoRequest request) {
        
        log.info("Updating video: {}", videoId);
        var updated = videoService.updateVideo(videoId, request);
        
        return ResponseEntity.ok(ApiResponse.success(updated, "Video updated"));
    }

    @DeleteMapping("/{videoId}")
    @Operation(summary = "Remove a video")
    public ResponseEntity<ApiResponse<String>> deleteVideo(@PathVariable Long videoId) {
        log.info("Deleting video: {}", videoId);
        videoService.deleteVideo(videoId);
        return ResponseEntity.ok(ApiResponse.success("Video removed", "Success"));
    }
}

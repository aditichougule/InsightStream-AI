package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.VideoDTO;
import com.aivideoip.dto.VideoUploadRequest;
import com.aivideoip.dto.YouTubeImportRequest;
import com.aivideoip.service.VideoUploadService;
import com.aivideoip.service.YouTubeImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * REST API controller for video upload and YouTube import operations.
 * Handles file uploads and YouTube video imports with metadata extraction.
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Video Upload", description = "Video upload and YouTube import APIs")
public class VideoUploadController {

    private final VideoUploadService videoUploadService;
    private final YouTubeImportService youtubeImportService;

    /**
     * Upload a video file directly.
     * Accepts multipart form data with video file and metadata.
     *
     * @param file Video file to upload (required)
     * @param title Video title (required)
     * @param description Video description (optional)
     * @param authentication User authentication from JWT token
     * @return VideoDTO with upload status and details
     */
    @PostMapping(value = "/video", consumes = "multipart/form-data")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Upload a video file",
            description = "Upload a video file with metadata. Supports MP4, AVI, MKV, MOV, FLV formats. Maximum file size: 5GB")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Video uploaded successfully",
                    content = @Content(schema = @Schema(implementation = VideoDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input or file validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "413",
                    description = "File size exceeds maximum limit"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "415",
                    description = "Unsupported media type"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<VideoDTO>> uploadVideo(
            @RequestParam("file") @NotNull(message = "Video file is required") MultipartFile file,
            @RequestParam("title") @NotBlank(message = "Title is required") String title,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {

        Long userId = extractUserIdFromAuth(authentication);
        log.info("Video upload request - User: {}, File: {}, Title: {}",
                userId, file.getOriginalFilename(), title);

        try {
            VideoDTO uploadedVideo = videoUploadService.uploadVideoFile(
                    file, title, description, userId);

            log.info("Video uploaded successfully - ID: {}", uploadedVideo.getId());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(
                            true,
                            "Video uploaded successfully",
                            uploadedVideo,
                            HttpStatus.CREATED.value()));

        } catch (IllegalArgumentException e) {
            log.warn("File validation failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(
                            false,
                            "File validation failed: " + e.getMessage(),
                            null,
                            HttpStatus.BAD_REQUEST.value()));

        } catch (Exception e) {
            log.error("Video upload failed: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to upload video: " + e.getMessage(),
                            null,
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Import a video from YouTube URL.
     * Fetches metadata from YouTube without downloading the full video initially.
     *
     * @param request YouTube import request with URL and optional metadata
     * @param authentication User authentication from JWT token
     * @return VideoDTO with YouTube video information
     */
    @PostMapping("/youtube")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Import video from YouTube",
            description = "Import a video from YouTube URL. Extracts metadata including title, description, duration, and thumbnail URL. Full video download handled asynchronously.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "YouTube video imported successfully",
                    content = @Content(schema = @Schema(implementation = VideoDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid YouTube URL or request parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "YouTube metadata extraction failed or yt-dlp unavailable")
    })
    public ResponseEntity<ApiResponse<VideoDTO>> importFromYouTube(
            @Valid @RequestBody YouTubeImportRequest request,
            Authentication authentication) {

        Long userId = extractUserIdFromAuth(authentication);
        log.info("YouTube import request - User: {}, URL: {}", userId, request.getYoutubeUrl());

        try {
            VideoDTO importedVideo = youtubeImportService.importFromYouTube(
                    request.getYoutubeUrl(),
                    request.getTitle(),
                    request.getDescription(),
                    userId);

            log.info("YouTube video imported successfully - ID: {}", importedVideo.getId());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(
                            true,
                            "YouTube video imported successfully",
                            importedVideo,
                            HttpStatus.CREATED.value()));

        } catch (IllegalArgumentException e) {
            log.warn("YouTube import validation failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(
                            false,
                            "Invalid request: " + e.getMessage(),
                            null,
                            HttpStatus.BAD_REQUEST.value()));

        } catch (RuntimeException e) {
            log.error("YouTube metadata extraction failed: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to fetch YouTube metadata: " + e.getMessage(),
                            null,
                            HttpStatus.SERVICE_UNAVAILABLE.value()));

        } catch (Exception e) {
            log.error("YouTube import failed: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to import YouTube video: " + e.getMessage(),
                            null,
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Extract user ID from JWT authentication token.
     * The user ID is stored as the principal in the authentication object.
     */
    private Long extractUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            log.warn("Authentication not found in request");
            throw new IllegalArgumentException("User not authenticated");
        }

        try {
            return Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            log.error("Invalid user ID in authentication: {}", authentication.getPrincipal());
            throw new IllegalArgumentException("Invalid user ID in token");
        }
    }
}

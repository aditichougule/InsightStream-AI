package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.service.AudioExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for audio extraction operations.
 * Handles extraction of audio from video files using FFmpeg.
 */
@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audio Extraction", description = "Audio extraction and conversion APIs")
public class AudioExtractionController {

    private final AudioExtractionService audioExtractionService;

    /**
     * Extract audio from an uploaded video file.
     * Creates an MP3 (or configured format) file from the video.
     *
     * @param videoId ID of the video to extract audio from
     * @return Path to the extracted audio file
     */
    @PostMapping("/extract/{videoId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Extract audio from video",
            description = "Extract audio from a video file and save as MP3. Returns the path to the extracted audio file.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Audio extracted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid video or unsupported source"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Video not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "FFmpeg unavailable or extraction failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<String>> extractAudio(@PathVariable Long videoId) {
        log.info("Audio extraction request for video: {}", videoId);

        try {
            // Check if FFmpeg is available
            if (!audioExtractionService.isFfmpegAvailable()) {
                log.warn("FFmpeg is not available on the system");
                return ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new ApiResponse<>(
                                false,
                                "FFmpeg is not available on the system",
                                null,
                                HttpStatus.SERVICE_UNAVAILABLE.value()));
            }

            // Extract audio
            String audioPath = audioExtractionService.extractAudioFromVideo(videoId);

            log.info("Audio extracted successfully - VideoID: {}, Path: {}", videoId, audioPath);

            return ResponseEntity
                    .ok(new ApiResponse<>(
                            true,
                            "Audio extracted successfully",
                            audioPath,
                            HttpStatus.OK.value()));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid audio extraction request: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(
                            false,
                            "Invalid request: " + e.getMessage(),
                            null,
                            HttpStatus.BAD_REQUEST.value()));

        } catch (Exception e) {
            log.error("Audio extraction failed: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to extract audio: " + e.getMessage(),
                            null,
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Check if FFmpeg is available on the system.
     * Useful for health checks and client-side decision making.
     *
     * @return true if FFmpeg is available, false otherwise
     */
    @GetMapping("/status")
    @Operation(summary = "Check audio extraction service status",
            description = "Check if FFmpeg is available and audio extraction service is ready")
    public ResponseEntity<ApiResponse<Boolean>> getAudioServiceStatus() {
        boolean available = audioExtractionService.isFfmpegAvailable();

        log.debug("Audio service status check - FFmpeg available: {}", available);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                available ? "Audio extraction service is ready" : "FFmpeg not available",
                available,
                HttpStatus.OK.value()));
    }
}

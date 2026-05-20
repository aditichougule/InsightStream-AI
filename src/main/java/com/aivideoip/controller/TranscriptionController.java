package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.TranscriptChunkDTO;
import com.aivideoip.service.TranscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API controller for video transcription operations.
 * Handles transcription requests via Whisper Python microservice.
 */
@RestController
@RequestMapping("/api/transcription")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transcription", description = "Convert audio to text using Whisper")
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    /**
     * Transcribe audio from a video.
     * Calls the Whisper Python microservice and stores transcript chunks.
     *
     * @param videoId ID of the video to transcribe
     * @return List of transcript chunks with timestamps
     */
    @PostMapping("/transcribe/{videoId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Transcribe video audio",
            description = "Transcribes audio using Whisper microservice. " +
                    "Audio must be extracted first using /api/audio/extract/{videoId}. " +
                    "Returns transcript segments with start/end timestamps.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transcription completed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Audio not extracted or service disabled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Video not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "Whisper service unavailable"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<TranscriptChunkDTO>>> transcribeVideo(
            @PathVariable Long videoId) {

        log.info("Transcription request - VideoID: {}", videoId);

        try {
            List<TranscriptChunkDTO> chunks = transcriptionService.transcribeAudio(videoId);

            log.info("Transcription successful - VideoID: {}, Chunks: {}", videoId, chunks.size());

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Video transcribed successfully",
                    chunks,
                    HttpStatus.OK.value()));

        } catch (IllegalArgumentException e) {
            log.warn("Transcription validation failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(
                            false,
                            e.getMessage(),
                            null,
                            HttpStatus.BAD_REQUEST.value()));

        } catch (Exception e) {
            log.error("Transcription failed: {}", e.getMessage(), e);

            // Return 503 if Whisper service is unavailable
            if (e.getMessage().contains("Whisper") || e.getMessage().contains("service")) {
                return ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new ApiResponse<>(
                                false,
                                "Transcription service unavailable: " + e.getMessage(),
                                null,
                                HttpStatus.SERVICE_UNAVAILABLE.value()));
            }

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            false,
                            "Transcription failed: " + e.getMessage(),
                            null,
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get all transcript chunks for a video.
     *
     * @param videoId ID of the video
     * @return List of transcript chunks ordered by time
     */
    @GetMapping("/chunks/{videoId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get video transcript",
            description = "Returns all transcript chunks for a video with timestamps, " +
                    "ordered chronologically.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transcripts retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Video not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<TranscriptChunkDTO>>> getTranscripts(
            @PathVariable Long videoId) {

        log.debug("Fetching transcripts - VideoID: {}", videoId);

        try {
            List<TranscriptChunkDTO> chunks = transcriptionService.getVideoTranscripts(videoId);

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    chunks.isEmpty() ? "No transcripts found" : "Transcripts retrieved",
                    chunks,
                    HttpStatus.OK.value()));

        } catch (Exception e) {
            log.error("Failed to retrieve transcripts: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to retrieve transcripts: " + e.getMessage(),
                            null,
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Get full transcription text for a video.
     * Returns concatenated text of all transcript chunks.
     *
     * @param videoId ID of the video
     * @return Full transcription text
     */
    @GetMapping("/text/{videoId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get full transcription text",
            description = "Returns the complete transcription text for a video " +
                    "as a single concatenated string.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Full text retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Video not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<String>> getTranscriptionText(
            @PathVariable Long videoId) {

        log.debug("Fetching transcription text - VideoID: {}", videoId);

        try {
            String text = transcriptionService.getVideoTranscriptionText(videoId);

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    text.isEmpty() ? "No transcription found" : "Transcription text retrieved",
                    text,
                    HttpStatus.OK.value()));

        } catch (Exception e) {
            log.error("Failed to retrieve transcription text: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to retrieve transcription: " + e.getMessage(),
                            null,
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}

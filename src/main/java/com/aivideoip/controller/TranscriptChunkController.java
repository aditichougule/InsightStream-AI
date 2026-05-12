package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.TranscriptChunkDTO;
import com.aivideoip.service.TranscriptChunkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/videos/{videoId}/transcript-chunks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transcript Chunks", description = "Manage transcript chunks")
public class TranscriptChunkController {

    private final TranscriptChunkService service;

    @PostMapping
    @Operation(summary = "Create a new transcript chunk")
    public ResponseEntity<ApiResponse<TranscriptChunkDTO>> create(
            @PathVariable Long videoId,
            @Valid @RequestBody TranscriptChunkDTO dto) {
        log.info("Creating transcript chunk for video: {}", videoId);
        var chunk = service.createChunk(videoId, dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(chunk, "Transcript chunk created"));
    }

    @GetMapping
    @Operation(summary = "Get all transcript chunks for a video")
    public ResponseEntity<ApiResponse<Page<TranscriptChunkDTO>>> list(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Listing chunks for video: {}", videoId);
        var pageable = PageRequest.of(page, size);
        var chunks = service.getVideoChunks(videoId, pageable);
        return ResponseEntity.ok(ApiResponse.success(chunks));
    }

    @GetMapping("/{chunkId}")
    @Operation(summary = "Get a specific transcript chunk")
    public ResponseEntity<ApiResponse<TranscriptChunkDTO>> get(@PathVariable Long chunkId) {
        log.debug("Fetching chunk: {}", chunkId);
        var chunk = service.getChunkById(chunkId);
        return ResponseEntity.ok(ApiResponse.success(chunk));
    }

    @PutMapping("/{chunkId}")
    @Operation(summary = "Update a transcript chunk")
    public ResponseEntity<ApiResponse<TranscriptChunkDTO>> update(
            @PathVariable Long chunkId,
            @Valid @RequestBody TranscriptChunkDTO dto) {
        log.info("Updating chunk: {}", chunkId);
        var updated = service.updateChunk(chunkId, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Updated"));
    }

    @DeleteMapping("/{chunkId}")
    @Operation(summary = "Delete a transcript chunk")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long chunkId) {
        log.info("Deleting chunk: {}", chunkId);
        service.deleteChunk(chunkId);
        return ResponseEntity.ok(ApiResponse.success("Chunk deleted"));
    }
}
